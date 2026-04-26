package ticket_train.ticketeer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.repository.BilletRepository;
import ticket_train.ticketeer.repository.ClientRepository;
import ticket_train.ticketeer.repository.ServiceFerroviaireRepository;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MobileApiService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ServiceFerroviaireRepository serviceFerroviaireRepository;
    private final BilletRepository billetRepository;
    private final MobileAuthService mobileAuthService;
    private final TarificationService tarificationService;
    private final MobileTicketService mobileTicketService;
    private final ClientTokenService clientTokenService;

    @Autowired
    public MobileApiService(
            ServiceFerroviaireRepository serviceFerroviaireRepository,
            BilletRepository billetRepository,
            MobileAuthService mobileAuthService,
            TarificationService tarificationService,
            MobileTicketService mobileTicketService,
            ClientTokenService clientTokenService
    ) {
        this.serviceFerroviaireRepository = serviceFerroviaireRepository;
        this.billetRepository = billetRepository;
        this.mobileAuthService = mobileAuthService;
        this.tarificationService = tarificationService;
        this.mobileTicketService = mobileTicketService;
        this.clientTokenService = clientTokenService;
    }

    MobileApiService(
            ClientRepository clientRepository,
            ServiceFerroviaireRepository serviceFerroviaireRepository,
            BilletRepository billetRepository,
            ticket_train.ticketeer.repository.SegmentBilletRepository segmentBilletRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            ClientTokenService clientTokenService,
            SignedQrService signedQrService
    ) {
        TarificationService localTarificationService = new TarificationService();
        this.serviceFerroviaireRepository = serviceFerroviaireRepository;
        this.billetRepository = billetRepository;
        this.mobileAuthService = new MobileAuthService(clientRepository, passwordEncoder, clientTokenService);
        this.tarificationService = localTarificationService;
        this.mobileTicketService = new MobileTicketService(
                billetRepository,
                segmentBilletRepository,
                signedQrService,
                localTarificationService
        );
        this.clientTokenService = clientTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return mobileAuthService.register(request);
    }

    public AuthResponse login(LoginRequest request) {
        return mobileAuthService.login(request);
    }

    public List<ServiceResponse> getServices() {
        List<ServiceResponse> responses = new ArrayList<>();
        for (ServiceFerroviaire service : serviceFerroviaireRepository.findAll()) {
            responses.add(toServiceResponse(service));
        }
        return responses;
    }

    public ClientProfileResponse getProfile(String token, String clientId) {
        return mobileAuthService.getProfile(token, clientId);
    }

    @Transactional
    public ClientProfileResponse updateProfile(String token, String clientId, UpdateProfileRequest request) {
        return mobileAuthService.updateProfile(token, clientId, request);
    }

    public TarificationResponse calculerTarif(String token, AchatBilletRequest request) {
        mobileAuthService.requireAuthorizedClient(token, request.getClientId());
        ServiceFerroviaire service = requireService(request.getServiceId());
        Optional<ServiceFerroviaire> returnService = resolveReturnService(request.getReturnServiceId(), service);
        return tarificationService.buildTarificationResponse(
                service,
                returnService,
                request.getProfilTarifaire(),
                request.getClasseReservation()
        );
    }

    @Transactional
    public AchatBilletResponse confirmerAchat(String token, AchatBilletRequest request) {
        Client client = mobileAuthService.requireAuthorizedClient(token, request.getClientId());
        ServiceFerroviaire service = requireService(request.getServiceId());
        Optional<ServiceFerroviaire> returnService = resolveReturnService(request.getReturnServiceId(), service);
        return mobileTicketService.createTicket(
                client,
                service,
                returnService,
                request,
                tarificationService.parseProfil(request.getProfilTarifaire())
        );
    }

    public TicketResponse getBillet(String token, String billetId) {
        Billet billet = requireAuthorizedTicket(token, billetId);
        return mobileTicketService.toTicketResponse(billet);
    }

    public List<TicketResponse> getBilletsByClient(String token, String clientId) {
        Client client = mobileAuthService.requireAuthorizedClient(token, clientId);
        return mobileTicketService.toTicketResponses(billetRepository.findByClientClientId(client.getClientId()));
    }

    public byte[] buildTicketPdf(String token, String billetId) {
        TicketResponse ticket = getBillet(token, billetId);
        return mobileTicketService.buildTicketPdf(ticket);
    }

    @Transactional
    public TicketResponse cancelBillet(String token, String billetId) {
        Billet billet = requireAuthorizedTicket(token, billetId);
        return mobileTicketService.cancelTicket(billet);
    }

    @Transactional
    public TicketResponse updateBillet(String token, String billetId, AchatBilletRequest request) {
        Billet billet = requireAuthorizedTicket(token, billetId);
        return mobileTicketService.updateTicket(billet, request);
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

    private UUID parseUuid(String value, String message) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String formatTime(LocalTime value) {
        return value != null ? value.format(TIME_FORMATTER) : null;
    }

    private String blankSafe(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
