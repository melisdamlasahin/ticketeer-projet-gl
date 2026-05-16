package ticket_train.ticketeer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationFailClosedTest {

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
    void missingServiceIdNeverReturnsValid() {
        ValidationResponse response = validationService.validerBillet(new ValidationRequest("CODE", null, UUID.randomUUID()), controleur);

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT, response.getMotif());
        verify(billetRepository, never()).findByCodeOptiqueForUpdate(any());
    }

    @Test
    void missingCheckpointIdNeverReturnsValid() {
        ValidationResponse response = validationService.validerBillet(new ValidationRequest("CODE", serviceId), controleur);

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT, response.getMotif());
        verify(billetRepository, never()).findByCodeOptiqueForUpdate(any());
    }

    @Test
    void missingCheckpointRecordNeverReturnsValid() {
        Billet billet = buildBillet(serviceId);
        UUID checkpointId = UUID.randomUUID();
        when(signedQrService.parseAndVerify("CODE")).thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptiqueForUpdate("CODE")).thenReturn(Optional.of(billet));
        when(serviceCheckpointRepository.findById(checkpointId)).thenReturn(Optional.empty());

        ValidationResponse response = validationService.validerBillet(new ValidationRequest("CODE", serviceId, checkpointId), controleur);

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.NON_CONFORME_SERVICE, response.getMotif());
    }

    @Test
    void databaseFailureReturnsTemporaryImpossible() {
        UUID checkpointId = UUID.randomUUID();
        when(signedQrService.parseAndVerify("CODE")).thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptiqueForUpdate("CODE")).thenThrow(new DataAccessResourceFailureException("db unavailable"));

        ValidationResponse response = validationService.validerBillet(new ValidationRequest("CODE", serviceId, checkpointId), controleur);

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT, response.getMotif());
    }

    @Test
    void checkpointFromAnotherServiceNeverReturnsValid() {
        Billet billet = buildBillet(serviceId);
        UUID checkpointId = UUID.randomUUID();
        ServiceCheckpoint checkpoint = buildCheckpoint(billet.getSegments().get(0), checkpointId, 1);
        ServiceFerroviaire otherService = new ServiceFerroviaire(
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                new Train("OTHER-" + UUID.randomUUID(), "Other"),
                new Ville("Other-A"),
                new Ville("Other-B"),
                10.0
        );
        otherService.setServiceId(UUID.randomUUID());
        checkpoint.setService(otherService);
        when(signedQrService.parseAndVerify("CODE")).thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptiqueForUpdate("CODE")).thenReturn(Optional.of(billet));
        when(serviceCheckpointRepository.findById(checkpointId)).thenReturn(Optional.of(checkpoint));

        ValidationResponse response = validationService.validerBillet(new ValidationRequest("CODE", serviceId, checkpointId), controleur);

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.NON_CONFORME_SERVICE, response.getMotif());
    }

    private Billet buildBillet(UUID currentServiceId) {
        Client client = new Client("Dupont", "Jean", "failclosed-" + UUID.randomUUID() + "@test", "hash", "photo");
        Train train = new Train("T-" + UUID.randomUUID(), "TGV Test");
        ServiceFerroviaire service = new ServiceFerroviaire(LocalDate.of(2026, 5, 10), LocalTime.of(9, 0), train, new Ville("Paris"), new Ville("Lyon"), 50.0);
        service.setServiceId(currentServiceId);
        Billet billet = new Billet("CODE", BigDecimal.TEN, client);
        billet.setEtat(TicketStatus.DISPONIBLE);
        SegmentBillet segment = new SegmentBillet(1, service);
        segment.setBillet(billet);
        segment.setEtatSegment(SegmentStatus.PREVU);
        segment.setOrdreDepartValide(1);
        segment.setOrdreArriveeValide(2);
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
