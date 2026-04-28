package ticket_train.ticketeer.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ticket_train.ticketeer.dto.mobile.AchatBilletRequest;
import ticket_train.ticketeer.dto.mobile.AchatBilletResponse;
import ticket_train.ticketeer.dto.mobile.TicketResponse;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceCheckpoint;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.model.enums.ProfilTarifaire;
import ticket_train.ticketeer.model.enums.TicketStatus;
import ticket_train.ticketeer.repository.BilletRepository;
import ticket_train.ticketeer.repository.SegmentBilletRepository;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;

@Service
public class MobileTicketService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final BilletRepository billetRepository;
    private final SegmentBilletRepository segmentBilletRepository;
    private final SignedQrService signedQrService;
    private final TarificationService tarificationService;

    public MobileTicketService(BilletRepository billetRepository,
                               SegmentBilletRepository segmentBilletRepository,
                               SignedQrService signedQrService,
                               TarificationService tarificationService) {
        this.billetRepository = billetRepository;
        this.segmentBilletRepository = segmentBilletRepository;
        this.signedQrService = signedQrService;
        this.tarificationService = tarificationService;
    }

    @Transactional
    public AchatBilletResponse createTicket(Client client,
                                            ServiceFerroviaire outboundService,
                                            Optional<ServiceFerroviaire> returnService,
                                            AchatBilletRequest request,
                                            ProfilTarifaire profilTarifaire) {
        double prixBase = tarificationService.computeBasePrice(outboundService, returnService, request.getClasseReservation());
        double prixFinal = tarificationService.applyDiscount(prixBase, profilTarifaire);

        Billet billet = new Billet(generateOpticalCode(), BigDecimal.valueOf(prixFinal), client);
        billet.setEtat(TicketStatus.DISPONIBLE);
        billet.setClasseReservation(tarificationService.normalizeClass(request.getClasseReservation()));
        billet.setNumeroPlace(generateSeatNumber(request.getPreferencePlace(), billet.getClasseReservation()));
        billet.setNomPassager(blankSafe(request.getNomPassager(), client.getPrenom() + " " + client.getNom()).trim());
        billet.setEmailPassager(blankSafe(request.getEmailPassager(), client.getEmail()));
        billet.setTelephonePassager(blankSafe(request.getTelephonePassager(), client.getTelephone()));
        billet.setMethodePaiement(blankSafe(request.getMethodePaiement(), "CARTE"));
        billet.setConfirmationEmailEnvoyee(Boolean.TRUE);
        billetRepository.save(billet);

        createSegment(billet, 1, outboundService);
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

    public List<TicketResponse> toTicketResponses(List<Billet> billets) {
        List<TicketResponse> responses = new ArrayList<>();
        for (Billet billet : billets) {
            responses.add(toTicketResponse(billet));
        }
        return responses;
    }

    public TicketResponse toTicketResponse(Billet billet) {
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

    public byte[] buildTicketPdf(TicketResponse ticket) {
        BufferedImage pageImage = renderTicketPage(ticket);
        return buildImagePdf(pageImage);
    }

    @Transactional
    public TicketResponse cancelTicket(Billet billet) {
        billet.setEtat(TicketStatus.ANNULE);
        billetRepository.save(billet);
        return toTicketResponse(billet);
    }

    @Transactional
    public TicketResponse updateTicket(Billet billet, AchatBilletRequest request) {
        if (billet.getEtat() == TicketStatus.ANNULE || billet.getEtat() == TicketStatus.TERMINE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billet non modifiable");
        }
        billet.setClasseReservation(tarificationService.normalizeClass(firstNonBlank(request.getClasseReservation(), billet.getClasseReservation())));
        String seatPreference = firstNonBlank(request.getPreferencePlace(), null);
        if (seatPreference != null) {
            billet.setNumeroPlace(generateSeatNumber(seatPreference, billet.getClasseReservation()));
        } else if (billet.getNumeroPlace() == null || billet.getNumeroPlace().isBlank()) {
            billet.setNumeroPlace(generateSeatNumber(null, billet.getClasseReservation()));
        }
        billet.setNomPassager(firstNonBlank(request.getNomPassager(), billet.getNomPassager()));
        billet.setEmailPassager(firstNonBlank(request.getEmailPassager(), billet.getEmailPassager()));
        billet.setTelephonePassager(firstNonBlank(request.getTelephonePassager(), billet.getTelephonePassager()));
        billet.setMethodePaiement(firstNonBlank(request.getMethodePaiement(), billet.getMethodePaiement()));
        billet.setConfirmationEmailEnvoyee(Boolean.TRUE);
        billetRepository.save(billet);
        return toTicketResponse(billet);
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

    private String generateSeatNumber(String preference, String bookingClass) {
        String coachPrefix = "PREMIERE".equalsIgnoreCase(tarificationService.normalizeClass(bookingClass)) ? "A" : "B";
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
            code = "TICK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (billetRepository.existsByCodeOptique(code));
        return code;
    }

    private String buildTicketImageBase64(Billet billet) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(signedQrService.buildSignedPayload(billet), BarcodeFormat.QR_CODE, 320, 320);
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException | WriterException ex) {
            return null;
        }
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
        graphics.drawString("Billet Ticketeer", x, y);

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
        for (int y = 0; y < image.getHeight(); y++) {
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
