package ticket_train.ticketeer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ticket_train.ticketeer.dto.ValidationRequest;
import ticket_train.ticketeer.dto.ValidationResponse;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceCheckpoint;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.model.Train;
import ticket_train.ticketeer.model.Ville;
import ticket_train.ticketeer.model.enums.SegmentStatus;
import ticket_train.ticketeer.model.enums.TicketStatus;
import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.repository.BilletRepository;
import ticket_train.ticketeer.repository.SegmentBilletRepository;
import ticket_train.ticketeer.repository.ServiceCheckpointRepository;
import ticket_train.ticketeer.repository.ServiceFerroviaireRepository;
import ticket_train.ticketeer.repository.ValidationRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SegmentStateTransitionTest {

    @Mock private BilletRepository billetRepository;
    @Mock private SegmentBilletRepository segmentBilletRepository;
    @Mock private ServiceCheckpointRepository serviceCheckpointRepository;
    @Mock private ValidationRepository validationRepository;
    @Mock private ServiceFerroviaireRepository serviceFerroviaireRepository;
    @Mock private SignedQrService signedQrService;
    @Mock private FraudDetectionService fraudDetectionService;
    @Mock private ValidationTraceService validationTraceService;
    @Mock private SecurityAuditService securityAuditService;

    private ValidationService validationService;
    private Controleur controleur;
    private UUID serviceId;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService(
                billetRepository,
                segmentBilletRepository,
                serviceCheckpointRepository,
                validationRepository,
                serviceFerroviaireRepository,
                signedQrService,
                fraudDetectionService,
                validationTraceService,
                securityAuditService,
                Clock.fixed(LocalDateTime.of(2026, 5, 10, 10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        );
        controleur = new Controleur("ctrl", "hash", "Nom", "Prenom");
        serviceId = UUID.randomUUID();
    }

    @Test
    void prevuBecomesValideAtIntermediateCheckpoint() {
        Billet billet = stubBilletAtCheckpoint(SegmentStatus.PREVU, 1, 3, 2);

        ValidationResponse response = validationService.validerBillet(requestForCurrentCheckpoint(), controleur);

        assertEquals(ValidationResult.VALID, response.getResultat());
        assertEquals(SegmentStatus.VALIDE, billet.getSegments().get(0).getEtatSegment());
        assertEquals(TicketStatus.EN_UTILISATION, billet.getEtat());
    }

    @Test
    void prevuBecomesTermineAtArrivalCheckpoint() {
        Billet billet = stubBilletAtCheckpoint(SegmentStatus.PREVU, 1, 2, 2);

        ValidationResponse response = validationService.validerBillet(requestForCurrentCheckpoint(), controleur);

        assertEquals(ValidationResult.VALID, response.getResultat());
        assertEquals(SegmentStatus.TERMINE, billet.getSegments().get(0).getEtatSegment());
        assertEquals(TicketStatus.TERMINE, billet.getEtat());
    }

    @Test
    void valideBecomesTermineAtLaterArrivalCheckpoint() {
        Billet billet = stubBilletAtCheckpoint(SegmentStatus.VALIDE, 1, 3, 3);

        ValidationResponse response = validationService.validerBillet(requestForCurrentCheckpoint(), controleur);

        assertEquals(ValidationResult.VALID, response.getResultat());
        assertEquals(SegmentStatus.TERMINE, billet.getSegments().get(0).getEtatSegment());
    }

    @Test
    void termineCannotBecomeValidAgain() {
        stubBilletAtCheckpoint(SegmentStatus.TERMINE, 1, 2, 2);
        when(fraudDetectionService.detectValidationIssue(any(), any(), any())).thenReturn(ValidationMotif.DEJA_VALIDE);

        ValidationResponse response = validationService.validerBillet(requestForCurrentCheckpoint(), controleur);

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.DEJA_VALIDE, response.getMotif());
    }

    @Test
    void terminatedLyonTicketCannotBeUsedAtMarseilleCheckpoint() {
        Billet billet = stubBilletAtCheckpoint(SegmentStatus.TERMINE, 1, 2, 3);

        ValidationResponse response = validationService.validerBillet(requestForCurrentCheckpoint(), controleur);

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.HORS_PARCOURS_AUTORISE, response.getMotif());
        assertEquals(SegmentStatus.TERMINE, billet.getSegments().get(0).getEtatSegment());
    }

    @Test
    void invalideCannotBecomeValidAgain() {
        stubBilletAtCheckpoint(SegmentStatus.INVALIDE, 1, 2, 1);
        when(fraudDetectionService.detectValidationIssue(any(), any(), any())).thenReturn(ValidationMotif.HORS_PARCOURS_AUTORISE);

        ValidationResponse response = validationService.validerBillet(requestForCurrentCheckpoint(), controleur);

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.HORS_PARCOURS_AUTORISE, response.getMotif());
    }

    private UUID currentCheckpointId;

    private Billet stubBilletAtCheckpoint(SegmentStatus status, int startOrder, int endOrder, int checkpointOrder) {
        Billet billet = buildBillet(status, startOrder, endOrder);
        currentCheckpointId = UUID.randomUUID();
        ServiceCheckpoint checkpoint = buildCheckpoint(billet.getSegments().get(0), currentCheckpointId, checkpointOrder);
        when(signedQrService.parseAndVerify("CODE")).thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptiqueForUpdate("CODE")).thenReturn(Optional.of(billet));
        when(serviceCheckpointRepository.findById(currentCheckpointId)).thenReturn(Optional.of(checkpoint));
        if (status == SegmentStatus.PREVU || status == SegmentStatus.VALIDE) {
            when(fraudDetectionService.detectValidationIssue(any(), any(), any())).thenReturn(null);
        }
        return billet;
    }

    private ValidationRequest requestForCurrentCheckpoint() {
        return new ValidationRequest("CODE", serviceId, currentCheckpointId);
    }

    private Billet buildBillet(SegmentStatus status, int startOrder, int endOrder) {
        Client client = new Client("Dupont", "Jean", "transition-" + UUID.randomUUID() + "@test", "hash", "photo");
        Train train = new Train("T-" + UUID.randomUUID(), "TGV Test");
        ServiceFerroviaire service = new ServiceFerroviaire(LocalDate.of(2026, 5, 10), LocalTime.of(9, 0), train, new Ville("Paris"), new Ville("Lyon"), 50.0);
        service.setServiceId(serviceId);
        Billet billet = new Billet("CODE", BigDecimal.TEN, client);
        billet.setEtat(TicketStatus.DISPONIBLE);
        SegmentBillet segment = new SegmentBillet(1, service);
        segment.setBillet(billet);
        segment.setEtatSegment(status);
        segment.setOrdreDepartValide(startOrder);
        segment.setOrdreArriveeValide(endOrder);
        billet.setSegments(List.of(segment));
        return billet;
    }

    private ServiceCheckpoint buildCheckpoint(SegmentBillet segment, UUID checkpointId, int order) {
        ServiceCheckpoint checkpoint = new ServiceCheckpoint();
        checkpoint.setCheckpointId(checkpointId);
        checkpoint.setService(segment.getService());
        checkpoint.setVille(new Ville("CP-" + order));
        checkpoint.setOrdre(order);
        return checkpoint;
    }
}
