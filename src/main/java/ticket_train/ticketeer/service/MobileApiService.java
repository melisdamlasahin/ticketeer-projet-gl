package ticket_train.ticketeer.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ticket_train.ticketeer.dto.mobile.AchatBilletRequest;
import ticket_train.ticketeer.dto.mobile.AchatBilletResponse;
import ticket_train.ticketeer.dto.mobile.AuthResponse;
import ticket_train.ticketeer.dto.mobile.ClientProfileResponse;
import ticket_train.ticketeer.dto.mobile.LoginRequest;
import ticket_train.ticketeer.dto.mobile.RegisterRequest;
import ticket_train.ticketeer.dto.mobile.ServiceResponse;
import ticket_train.ticketeer.dto.mobile.TarificationResponse;
import ticket_train.ticketeer.dto.mobile.TicketResponse;
import ticket_train.ticketeer.dto.mobile.UpdateProfileRequest;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceCheckpoint;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.model.enums.ProfilTarifaire;
import ticket_train.ticketeer.model.enums.TicketStatus;
import ticket_train.ticketeer.repository.BilletRepository;
import ticket_train.ticketeer.repository.ClientRepository;
import ticket_train.ticketeer.repository.SegmentBilletRepository;
import ticket_train.ticketeer.repository.ServiceFerroviaireRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;

@Service
@Transactional(readOnly = true)
public class MobileApiService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ClientRepository clientRepository;
    private final ServiceFerroviaireRepository serviceFerroviaireRepository;
    private final BilletRepository billetRepository;
    private final SegmentBilletRepository segmentBilletRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientTokenService clientTokenService;
    private final SignedQrService signedQrService;

    public MobileApiService(
            ClientRepository clientRepository,
            ServiceFerroviaireRepository serviceFerroviaireRepository,
            BilletRepository billetRepository,
            SegmentBilletRepository segmentBilletRepository,
            PasswordEncoder passwordEncoder,
            ClientTokenService clientTokenService,
            SignedQrService signedQrService
    ) {
        this.clientRepository = clientRepository;
        this.serviceFerroviaireRepository = serviceFerroviaireRepository;
        this.billetRepository = billetRepository;
        this.segmentBilletRepository = segmentBilletRepository;
        this.passwordEncoder = passwordEncoder;
        this.clientTokenService = clientTokenService;
        this.signedQrService = signedQrService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalized(request.getEmail());
        if (email == null || request.getMotDePasse() == null || request.getMotDePasse().isBlank()) {
            return authFailure("Informations d'inscription invalides");
        }
        if (clientRepository.findByEmail(email).isPresent()) {
            return authFailure("Un compte existe déjà avec cet email");
        }

        Client client = new Client(
                blankSafe(request.getNom(), "Utilisateur"),
                blankSafe(request.getPrenom(), ""),
                email,
                passwordEncoder.encode(request.getMotDePasse()),
                "photos/default-client.png"
        );
        clientRepository.save(client);
        return buildAuthSuccess(client, "Inscription réussie");
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalized(request.getEmail());
        Optional<Client> clientOpt = email == null ? Optional.empty() : clientRepository.findByEmail(email);
        if (clientOpt.isEmpty()) {
            return authFailure("Identifiants invalides");
        }

        Client client = clientOpt.get();
        String hash = client.getHashMotDePasse();
        if (hash == null || request.getMotDePasse() == null || !passwordEncoder.matches(request.getMotDePasse(), hash)) {
            return authFailure("Identifiants invalides");
        }

        return buildAuthSuccess(client, "Connexion réussie");
    }

    public List<ServiceResponse> getServices() {
        List<ServiceResponse> responses = new ArrayList<>();
        for (ServiceFerroviaire service : serviceFerroviaireRepository.findAll()) {
            responses.add(toServiceResponse(service));
        }
        return responses;
    }

    public ClientProfileResponse getProfile(String token, String clientId) {
        Client client = requireAuthorizedClient(token, clientId);
        return toProfileResponse(client);
    }

    @Transactional
    public ClientProfileResponse updateProfile(String token, String clientId, UpdateProfileRequest request) {
        Client client = requireAuthorizedClient(token, clientId);
        client.setNom(blankSafe(request.getNom(), client.getNom()));
        client.setPrenom(blankSafe(request.getPrenom(), client.getPrenom()));
        client.setSexe(blankToNull(request.getSexe()));
        client.setTelephone(blankToNull(request.getTelephone()));
        client.setDateNaissance(parseDate(blankToNull(request.getDateNaissance())));
        clientRepository.save(client);
        return toProfileResponse(client);
    }

    public TarificationResponse calculerTarif(String token, AchatBilletRequest request) {
        requireAuthorizedClient(token, request.getClientId());
        ServiceFerroviaire service = requireService(request.getServiceId());
        Optional<ServiceFerroviaire> returnService = resolveReturnService(request.getReturnServiceId(), service);
        ProfilTarifaire profilTarifaire = parseProfil(request.getProfilTarifaire());
        double prixBase = computeBasePrice(service, returnService, request.getClasseReservation());

        TarificationResponse response = new TarificationResponse();
        response.setServiceId(service.getServiceId().toString());
        response.setProfilTarifaire(profilTarifaire.name());
        response.setPrixBase(prixBase);
        response.setPrixFinal(applyDiscount(prixBase, profilTarifaire));
        return response;
    }

    @Transactional
    public AchatBilletResponse confirmerAchat(String token, AchatBilletRequest request) {
        Client client = requireAuthorizedClient(token, request.getClientId());
        ServiceFerroviaire service = requireService(request.getServiceId());
        Optional<ServiceFerroviaire> returnService = resolveReturnService(request.getReturnServiceId(), service);
        ProfilTarifaire profilTarifaire = parseProfil(request.getProfilTarifaire());
        double prixBase = computeBasePrice(service, returnService, request.getClasseReservation());
        double prixFinal = applyDiscount(prixBase, profilTarifaire);

        Billet billet = new Billet(generateOpticalCode(), BigDecimal.valueOf(prixFinal), client);
        billet.setEtat(TicketStatus.DISPONIBLE);
        billet.setClasseReservation(normalizeClass(request.getClasseReservation()));
        billet.setNumeroPlace(generateSeatNumber(request.getPreferencePlace(), billet.getClasseReservation()));
        billet.setNomPassager(blankSafe(request.getNomPassager(), client.getPrenom() + " " + client.getNom()).trim());
        billet.setEmailPassager(blankSafe(request.getEmailPassager(), client.getEmail()));
        billet.setTelephonePassager(blankSafe(request.getTelephonePassager(), client.getTelephone()));
        billet.setMethodePaiement(blankSafe(request.getMethodePaiement(), "CARTE"));
        billet.setConfirmationEmailEnvoyee(Boolean.TRUE);
        billetRepository.save(billet);

        createSegment(billet, 1, service);
        if (returnService.isPresent()) {
            createSegment(billet, 2, returnService.get());
        }
        billetRepository.save(billet);

        AchatBilletResponse response = new AchatBilletResponse();
        response.setSuccess(Boolean.TRUE);
        response.setMessage(returnService.isPresent()
                ? "Votre billet aller-retour a été généré avec succès."
                : "Votre billet a été généré avec succès.");
        response.setBilletId(billet.getTicketId().toString());
        response.setPrixFinal(prixFinal);
        return response;
    }

    public TicketResponse getBillet(String token, String billetId) {
        Billet billet = requireAuthorizedTicket(token, billetId);
        return toTicketResponse(billet);
    }

    public List<TicketResponse> getBilletsByClient(String token, String clientId) {
        Client client = requireAuthorizedClient(token, clientId);
        List<TicketResponse> responses = new ArrayList<>();
        for (Billet billet : billetRepository.findByClientClientId(client.getClientId())) {
            responses.add(toTicketResponse(billet));
        }
        return responses;
    }

    public byte[] buildTicketPdf(String token, String billetId) {
        TicketResponse ticket = getBillet(token, billetId);
        return buildTicketPdfWithQr(ticket);
    }

    @Transactional
    public TicketResponse cancelBillet(String token, String billetId) {
        Billet billet = requireAuthorizedTicket(token, billetId);
        billet.setEtat(TicketStatus.ANNULE);
        billetRepository.save(billet);
        return toTicketResponse(billet);
    }

    @Transactional
    public TicketResponse updateBillet(String token, String billetId, AchatBilletRequest request) {
        Billet billet = requireAuthorizedTicket(token, billetId);
        if (billet.getEtat() == TicketStatus.ANNULE || billet.getEtat() == TicketStatus.TERMINE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billet non modifiable");
        }
        billet.setClasseReservation(normalizeClass(firstNonBlank(request.getClasseReservation(), billet.getClasseReservation())));
        billet.setNumeroPlace(generateSeatNumber(firstNonBlank(request.getPreferencePlace(), billet.getNumeroPlace()), billet.getClasseReservation()));
        billet.setNomPassager(firstNonBlank(request.getNomPassager(), billet.getNomPassager()));
        billet.setEmailPassager(firstNonBlank(request.getEmailPassager(), billet.getEmailPassager()));
        billet.setTelephonePassager(firstNonBlank(request.getTelephonePassager(), billet.getTelephonePassager()));
        billet.setMethodePaiement(firstNonBlank(request.getMethodePaiement(), billet.getMethodePaiement()));
        billet.setConfirmationEmailEnvoyee(Boolean.TRUE);
        billetRepository.save(billet);
        return toTicketResponse(billet);
    }

    private AuthResponse buildAuthSuccess(Client client, String message) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setClientId(client.getClientId().toString());
        response.setAuthToken(clientTokenService.issueToken(client.getClientId()));
        response.setNom(client.getNom());
        response.setPrenom(client.getPrenom());
        response.setEmail(client.getEmail());
        response.setSexe(client.getSexe());
        response.setDateNaissance(client.getDateNaissance() != null ? client.getDateNaissance().toString() : null);
        response.setTelephone(client.getTelephone());
        return response;
    }

    private AuthResponse authFailure(String message) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    private ClientProfileResponse toProfileResponse(Client client) {
        ClientProfileResponse response = new ClientProfileResponse();
        response.setClientId(client.getClientId().toString());
        response.setNom(client.getNom());
        response.setPrenom(client.getPrenom());
        response.setEmail(client.getEmail());
        response.setSexe(client.getSexe());
        response.setDateNaissance(client.getDateNaissance() != null ? client.getDateNaissance().toString() : null);
        response.setTelephone(client.getTelephone());
        return response;
    }

    private ServiceResponse toServiceResponse(ServiceFerroviaire service) {
        ServiceResponse response = new ServiceResponse();
        response.setServiceId(service.getServiceId().toString());
        response.setDateTrajet(service.getDateTrajet().toString());
        response.setHeureDepart(formatTime(service.getHeureDepart()));
        response.setPrixBase(service.getPrixBase());
        response.setTrainNom(service.getTrain().getNomTrain());
        response.setVilleDepartNom(service.getVilleDepart().getNom());
        response.setVilleArriveeNom(service.getVilleArrivee().getNom());
        response.setVoie(blankSafe(service.getVoie(), "Voie à confirmer"));
        response.setRetardMinutes(service.getRetardMinutes() != null ? service.getRetardMinutes() : 0);
        return response;
    }

    private TicketResponse toTicketResponse(Billet billet) {
        TicketResponse response = new TicketResponse();
        response.setBilletId(billet.getTicketId().toString());
        response.setCodeOptique(billet.getCodeOptique());
        response.setPrixFinal(billet.getPrixFinal().doubleValue());
        response.setEtat(billet.getEtat().name());
        response.setDateEmission(billet.getDateEmission().toString());
        response.setClasseReservation(billet.getClasseReservation());
        response.setNumeroPlace(billet.getNumeroPlace());
        response.setNomPassager(billet.getNomPassager());
        response.setEmailPassager(billet.getEmailPassager());
        response.setTelephonePassager(billet.getTelephonePassager());
        response.setMethodePaiement(billet.getMethodePaiement());
        response.setConfirmationEmailEnvoyee(billet.getConfirmationEmailEnvoyee());

        List<SegmentBillet> orderedSegments = billet.getSegments().stream()
                .sorted(Comparator.comparing(SegmentBillet::getOrdre))
                .toList();

        SegmentBillet outboundSegment = orderedSegments.isEmpty() ? null : orderedSegments.get(0);
        if (outboundSegment != null) {
            ServiceFerroviaire service = outboundSegment.getService();
            response.setTrainNom(service.getTrain().getNomTrain());
            response.setVilleDepartNom(service.getVilleDepart().getNom());
            response.setVilleArriveeNom(service.getVilleArrivee().getNom());
            response.setDateTrajet(service.getDateTrajet().toString());
            response.setHeureDepart(formatTime(service.getHeureDepart()));
            response.setTrajetResume(service.getVilleDepart().getNom() + " → " + service.getVilleArrivee().getNom());
            response.setVoie(blankSafe(service.getVoie(), "Voie à confirmer"));
            response.setRetardMinutes(service.getRetardMinutes() != null ? service.getRetardMinutes() : 0);
        }

        if (orderedSegments.size() > 1) {
            SegmentBillet returnSegment = orderedSegments.get(1);
            ServiceFerroviaire returnService = returnSegment.getService();
            response.setTypeTrajet("ALLER_RETOUR");
            response.setTrajetResume(returnService.getVilleArrivee().getNom() + " ↔ " + returnService.getVilleDepart().getNom());
            response.setTrainRetourNom(returnService.getTrain().getNomTrain());
            response.setVilleRetourDepartNom(returnService.getVilleDepart().getNom());
            response.setVilleRetourArriveeNom(returnService.getVilleArrivee().getNom());
            response.setDateRetour(returnService.getDateTrajet().toString());
            response.setHeureRetour(formatTime(returnService.getHeureDepart()));
        } else {
            response.setTypeTrajet("ALLER_SIMPLE");
        }

        response.setQrCodeBase64(buildTicketImageBase64(billet));
        return response;
    }

    private Client requireAuthorizedClient(String token, String clientId) {
        UUID requestedId = parseUuid(clientId, "Client invalide");
        UUID tokenClientId = clientTokenService.resolveClientId(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Auth token invalide"));
        if (!tokenClientId.equals(requestedId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return clientRepository.findById(requestedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable"));
    }

    private Billet requireAuthorizedTicket(String token, String billetId) {
        UUID requestedId = parseUuid(billetId, "Billet invalide");
        UUID tokenClientId = clientTokenService.resolveClientId(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Auth token invalide"));
        Billet billet = billetRepository.findById(requestedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Billet introuvable"));
        if (!billet.getClient().getClientId().equals(tokenClientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return billet;
    }

    private ServiceFerroviaire requireService(String serviceId) {
        UUID id = parseUuid(serviceId, "Service invalide");
        return serviceFerroviaireRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable"));
    }

    private Optional<ServiceFerroviaire> resolveReturnService(String returnServiceId, ServiceFerroviaire outboundService) {
        if (returnServiceId == null || returnServiceId.isBlank()) {
            return Optional.empty();
        }

        ServiceFerroviaire returnService = requireService(returnServiceId);
        validateRoundTripCompatibility(outboundService, returnService);
        return Optional.of(returnService);
    }

    private void validateRoundTripCompatibility(ServiceFerroviaire outboundService, ServiceFerroviaire returnService) {
        boolean reverseRoute = outboundService.getVilleDepart().getNom().equalsIgnoreCase(returnService.getVilleArrivee().getNom())
                && outboundService.getVilleArrivee().getNom().equalsIgnoreCase(returnService.getVilleDepart().getNom());
        boolean validDates = !returnService.getDateTrajet().isBefore(outboundService.getDateTrajet());

        if (!reverseRoute || !validDates) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service retour incompatible");
        }
    }

    private void createSegment(Billet billet, int ordre, ServiceFerroviaire service) {
        SegmentBillet segment = new SegmentBillet(ordre, service);
        segment.setOrdreDepartValide(1);
        int maxCheckpointOrder = service.getCheckpoints() != null && !service.getCheckpoints().isEmpty()
                ? service.getCheckpoints().stream().map(ServiceCheckpoint::getOrdre).max(Integer::compareTo).orElse(2)
                : 2;
        segment.setOrdreArriveeValide(maxCheckpointOrder);
        segment.setBillet(billet);
        billet.getSegments().add(segment);
        segmentBilletRepository.save(segment);
    }

    private UUID parseUuid(String value, String message) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private ProfilTarifaire parseProfil(String value) {
        if (value == null || value.isBlank()) {
            return ProfilTarifaire.STANDARD;
        }
        try {
            return ProfilTarifaire.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ProfilTarifaire.STANDARD;
        }
    }

    private double applyDiscount(double prixBase, ProfilTarifaire profilTarifaire) {
        double coefficient = switch (profilTarifaire) {
            case ENFANT_MOINS_7 -> 0.25;
            case ETUDIANT_DECLARE -> 0.80;
            case SENIOR_65_PLUS -> 0.70;
            case HANDICAP_DECLARE -> 0.60;
            case STANDARD -> 1.00;
        };
        return BigDecimal.valueOf(prixBase * coefficient).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double computeBasePrice(ServiceFerroviaire service,
                                    Optional<ServiceFerroviaire> returnService,
                                    String classeReservation) {
        double prixBase = service.getPrixBase() + returnService.map(ServiceFerroviaire::getPrixBase).orElse(0.0);
        double classMultiplier = "PREMIERE".equalsIgnoreCase(normalizeClass(classeReservation)) ? 1.35 : 1.00;
        return BigDecimal.valueOf(prixBase * classMultiplier).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String normalizeClass(String value) {
        if (value == null || value.isBlank()) {
            return "SECONDE";
        }
        String normalized = value.trim().toUpperCase();
        return normalized.startsWith("PREM") ? "PREMIERE" : "SECONDE";
    }

    private String generateSeatNumber(String preference, String bookingClass) {
        String coachPrefix = "PREMIERE".equalsIgnoreCase(normalizeClass(bookingClass)) ? "A" : "B";
        String seatSuffix = "01";
        if (preference != null && !preference.isBlank()) {
            String normalized = preference.trim().toUpperCase();
            if (normalized.contains("FEN")) {
                seatSuffix = "12";
            } else if (normalized.contains("COULOIR")) {
                seatSuffix = "08";
            } else if (normalized.contains("CALME")) {
                seatSuffix = "02";
            }
        }
        return coachPrefix + "-" + seatSuffix;
    }

    private String firstNonBlank(String candidate, String fallback) {
        return candidate != null && !candidate.isBlank() ? candidate.trim() : fallback;
    }

    private String generateOpticalCode() {
        String code;
        do {
            code = "EASY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (billetRepository.existsByCodeOptique(code));
        return code;
    }

    private String buildTicketImageBase64(Billet billet) {
        String qrPayload = buildQrPayload(billet);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrPayload, BarcodeFormat.QR_CODE, 320, 320);
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException | WriterException ex) {
            return null;
        }
    }

    private String buildQrPayload(Billet billet) {
        return signedQrService.buildSignedPayload(billet);
    }

    private String buildReturnPdfBlock(TicketResponse ticket) {
        if (!"ALLER_RETOUR".equals(ticket.getTypeTrajet())) {
            return "";
        }
        return "Retour: " + safe(ticket.getVilleRetourDepartNom()) + " -> " + safe(ticket.getVilleRetourArriveeNom()) + "\n"
                + "Train retour: " + safe(ticket.getTrainRetourNom()) + "\n"
                + "Date retour: " + safe(ticket.getDateRetour()) + "\n"
                + "Heure retour: " + safe(ticket.getHeureRetour()) + "\n";
    }

    private byte[] buildTicketPdfWithQr(TicketResponse ticket) {
        BufferedImage pageImage = renderTicketPage(ticket);
        return buildImagePdf(pageImage);
    }

    private BufferedImage renderTicketPage(TicketResponse ticket) {
        int width = 595;
        int height = 842;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(new Color(30, 30, 30));
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = 40;
        int y = 60;
        graphics.setFont(new Font("SansSerif", Font.BOLD, 24));
        graphics.drawString("Billet EasyRail", x, y);

        y += 35;
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 14));
        for (String line : buildTicketPdfLines(ticket)) {
            graphics.drawString(line, x, y);
            y += 24;
        }

        BufferedImage qrImage = decodeQrImage(ticket.getQrCodeBase64());
        if (qrImage != null) {
            int qrSize = 180;
            graphics.drawImage(qrImage, width - qrSize - 40, 60, qrSize, qrSize, null);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 12));
            graphics.drawString("QR de validation", width - qrSize - 20, 260);
        }

        graphics.dispose();
        return image;
    }

    private List<String> buildTicketPdfLines(TicketResponse ticket) {
        List<String> lines = new ArrayList<>();
        lines.add("Billet : " + safe(ticket.getBilletId()));
        lines.add("Trajet : " + safe(ticket.getVilleDepartNom()) + " -> " + safe(ticket.getVilleArriveeNom()));
        lines.add("Train : " + safe(ticket.getTrainNom()));
        lines.add("Classe : " + safe(ticket.getClasseReservation()));
        lines.add("Place : " + safe(ticket.getNumeroPlace()));
        lines.add("Date : " + safe(ticket.getDateTrajet()));
        lines.add("Heure : " + safe(ticket.getHeureDepart()));
        lines.add("Voie : " + safe(ticket.getVoie()));
        if ("ALLER_RETOUR".equals(ticket.getTypeTrajet())) {
            lines.add("Retour : " + safe(ticket.getVilleRetourDepartNom()) + " -> " + safe(ticket.getVilleRetourArriveeNom()));
            lines.add("Train retour : " + safe(ticket.getTrainRetourNom()));
            lines.add("Date retour : " + safe(ticket.getDateRetour()));
            lines.add("Heure retour : " + safe(ticket.getHeureRetour()));
        }
        lines.add("Prix : " + safe(String.valueOf(ticket.getPrixFinal())) + " EUR");
        lines.add("Code : " + safe(ticket.getCodeOptique()));
        return lines;
    }

    private BufferedImage decodeQrImage(String qrCodeBase64) {
        if (qrCodeBase64 == null || qrCodeBase64.isBlank()) {
            return null;
        }
        byte[] decoded = Base64.getDecoder().decode(qrCodeBase64);
        try {
            return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(decoded));
        } catch (IOException ex) {
            return null;
        }
    }

    private byte[] buildImagePdf(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] rgbBytes = toRgbBytes(image);
        byte[] compressed = deflate(rgbBytes);
        String contentStream = "q\n" + width + " 0 0 " + height + " 0 0 cm\n/Im1 Do\nQ\n";

        String obj1 = "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n";
        String obj2 = "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n";
        String obj3 = "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 " + width + " " + height + "] "
                + "/Resources << /XObject << /Im1 4 0 R >> >> /Contents 5 0 R >> endobj\n";
        String obj4Header = "4 0 obj << /Type /XObject /Subtype /Image /Width " + width + " /Height " + height
                + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length " + compressed.length + " >> stream\n";
        String obj4Footer = "\nendstream endobj\n";
        byte[] obj4Bytes = concat(
                obj4Header.getBytes(StandardCharsets.ISO_8859_1),
                compressed,
                obj4Footer.getBytes(StandardCharsets.ISO_8859_1)
        );
        String obj5 = "5 0 obj << /Length " + contentStream.getBytes(StandardCharsets.ISO_8859_1).length
                + " >> stream\n" + contentStream + "endstream endobj\n";

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        writeString(pdf, "%PDF-1.4\n");
        offsets.add(pdf.size());
        writeString(pdf, obj1);
        offsets.add(pdf.size());
        writeString(pdf, obj2);
        offsets.add(pdf.size());
        writeString(pdf, obj3);
        offsets.add(pdf.size());
        writeBytes(pdf, obj4Bytes);
        offsets.add(pdf.size());
        writeString(pdf, obj5);

        int xrefStart = pdf.size();
        writeString(pdf, "xref\n0 6\n");
        writeString(pdf, "0000000000 65535 f \n");
        for (int i = 1; i <= 5; i++) {
            writeString(pdf, String.format("%010d 00000 n \n", offsets.get(i)));
        }
        writeString(pdf, "trailer << /Size 6 /Root 1 0 R >>\n");
        writeString(pdf, "startxref\n" + xrefStart + "\n%%EOF");
        return pdf.toByteArray();
    }

    private byte[] toRgbBytes(BufferedImage image) {
        byte[] bytes = new byte[image.getWidth() * image.getHeight() * 3];
        int index = 0;
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                bytes[index++] = (byte) ((rgb >> 16) & 0xFF);
                bytes[index++] = (byte) ((rgb >> 8) & 0xFF);
                bytes[index++] = (byte) (rgb & 0xFF);
            }
        }
        return bytes;
    }

    private byte[] deflate(byte[] input) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             DeflaterOutputStream deflater = new DeflaterOutputStream(output)) {
            deflater.write(input);
            deflater.finish();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de compresser le PDF", ex);
        }
    }

    private byte[] concat(byte[]... chunks) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (byte[] chunk : chunks) {
                output.write(chunk);
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de construire le PDF", ex);
        }
    }

    private void writeString(ByteArrayOutputStream output, String value) {
        writeBytes(output, value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private void writeBytes(ByteArrayOutputStream output, byte[] value) {
        try {
            output.write(value);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible d'ecrire le PDF", ex);
        }
    }

    private String formatTime(LocalTime value) {
        return value != null ? value.format(TIME_FORMATTER) : null;
    }

    private byte[] buildSimplePdf(String text) {
        String escaped = text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        String stream = "BT /F1 12 Tf 50 760 Td 14 TL (" + escaped.replace("\n", ") Tj T* (") + ") Tj ET";
        String obj1 = "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n";
        String obj2 = "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n";
        String obj3 = "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n";
        String obj4 = "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n";
        String obj5 = "5 0 obj << /Length " + stream.getBytes(StandardCharsets.ISO_8859_1).length + " >> stream\n"
                + stream + "\nendstream endobj\n";

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        offsets.add(pdf.length());
        pdf.append(obj1);
        offsets.add(pdf.length());
        pdf.append(obj2);
        offsets.add(pdf.length());
        pdf.append(obj3);
        offsets.add(pdf.length());
        pdf.append(obj4);
        offsets.add(pdf.length());
        pdf.append(obj5);
        int xrefStart = pdf.length();
        pdf.append("xref\n0 6\n");
        pdf.append("0000000000 65535 f \n");
        for (int i = 1; i <= 5; i++) {
            pdf.append(String.format("%010d 00000 n \n", offsets.get(i)));
        }
        pdf.append("trailer << /Size 6 /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefStart).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date invalide");
        }
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String blankSafe(String value, String fallback) {
        String normalizedValue = blankToNull(value);
        return normalizedValue != null ? normalizedValue : fallback;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
