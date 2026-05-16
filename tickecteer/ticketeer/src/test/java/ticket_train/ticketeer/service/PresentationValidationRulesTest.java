package ticket_train.ticketeer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Presentation demo: validation rules, signed QR and fail-closed behavior")
class PresentationValidationRulesTest {

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
                Clock.fixed(LocalDateTime.of(2026, 5, 15, 10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        );
        controleur = new Controleur("demo-controller", "bcrypt-hash", "Controller", "Demo");
        serviceId = UUID.randomUUID();
    }

    @Test
    @DisplayName("A copied and modified QR Code is rejected because the HMAC SHA-256 signature no longer matches")
    void tamperedSignedQrCodeIsRejected() {
        SignedQrService realSignedQrService = new SignedQrService("presentation-secret");
        String signedQrCode = realSignedQrService.buildSignedPayload(sampleBillet(serviceId));
        String tamperedQrCode = tamperPayloadWithoutUpdatingSignature(signedQrCode);

        SignedQrService.ParseResult result = realSignedQrService.parseAndVerify(tamperedQrCode);

        assertEquals(SignedQrService.ParseStatus.INVALID_SIGNATURE, result.getStatus());
        assertTrue(result.getPayload().isEmpty());
    }

    @Test
    @DisplayName("The system is fail-closed: a database failure never returns VALID")
    void databaseFailureReturnsTemporaryImpossible() {
        UUID checkpointId = UUID.randomUUID();
        when(signedQrService.parseAndVerify("CODE-DEMO"))
                .thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptiqueForUpdate("CODE-DEMO"))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        ValidationResponse response = validationService.validerBillet(
                new ValidationRequest("CODE-DEMO", serviceId, checkpointId),
                controleur
        );

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT, response.getMotif());
    }

    @Test
    @DisplayName("Anti-reuse by segment: first scan is accepted, second scan is DEJA_VALIDE")
    void sameSegmentCannotBeValidatedTwice() {
        UUID checkpointId = UUID.randomUUID();
        Billet billet = sampleBillet(serviceId);
        SegmentBillet segment = billet.getSegments().get(0);
        ServiceCheckpoint checkpoint = checkpointFor(segment, checkpointId, 1);

        when(signedQrService.parseAndVerify("CODE-DEMO"))
                .thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptiqueForUpdate("CODE-DEMO")).thenReturn(Optional.of(billet));
        when(serviceCheckpointRepository.findById(checkpointId)).thenReturn(Optional.of(checkpoint));
        when(fraudDetectionService.detectValidationIssue(any(), any(), any()))
                .thenReturn(null, ValidationMotif.DEJA_VALIDE);

        ValidationRequest request = new ValidationRequest("CODE-DEMO", serviceId, checkpointId);

        ValidationResponse firstScan = validationService.validerBillet(request, controleur);
        ValidationResponse secondScan = validationService.validerBillet(request, controleur);

        assertEquals(ValidationResult.VALID, firstScan.getResultat());
        assertEquals(SegmentStatus.VALIDE, segment.getEtatSegment());
        assertEquals(ValidationResult.INVALID, secondScan.getResultat());
        assertEquals(ValidationMotif.DEJA_VALIDE, secondScan.getMotif());
    }

    @Test
    @DisplayName("Each validation attempt is written to the audit trail")
    void everyValidationAttemptIsTraced() {
        UUID checkpointId = UUID.randomUUID();
        when(signedQrService.parseAndVerify("UNKNOWN-CODE"))
                .thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptiqueForUpdate("UNKNOWN-CODE")).thenReturn(Optional.empty());

        ValidationResponse response = validationService.validerBillet(
                new ValidationRequest("UNKNOWN-CODE", serviceId, checkpointId),
                controleur
        );

        assertEquals(ValidationMotif.BILLET_INCONNU, response.getMotif());
        verify(validationTraceService).saveAttemptTrace(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("An invalid signed QR Code is rejected before any ticket lookup")
    void invalidQrSignatureStopsBeforeTicketLookup() {
        UUID checkpointId = UUID.randomUUID();
        when(signedQrService.parseAndVerify("TAMPERED-QR"))
                .thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.INVALID_SIGNATURE, null));

        ValidationResponse response = validationService.validerBillet(
                new ValidationRequest("TAMPERED-QR", serviceId, checkpointId),
                controleur
        );

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.QR_SIGNATURE_INVALIDE, response.getMotif());
        verify(billetRepository, never()).findByCodeOptiqueForUpdate(any());
    }

    private String tamperPayloadWithoutUpdatingSignature(String signedQrCode) {
        String[] parts = signedQrCode.split("\\.", 3);
        String encodedPayload = parts[1];
        char lastChar = encodedPayload.charAt(encodedPayload.length() - 1);
        char replacement = lastChar == 'A' ? 'B' : 'A';
        String tamperedPayload = encodedPayload.substring(0, encodedPayload.length() - 1) + replacement;
        return parts[0] + "." + tamperedPayload + "." + parts[2];
    }

    private Billet sampleBillet(UUID currentServiceId) {
        Client client = new Client("Dupont", "Jean", "demo-" + UUID.randomUUID() + "@test", "hash", "photo");
        client.setClientId(UUID.randomUUID());

        Train train = new Train("T-" + UUID.randomUUID(), "TGV Demo");
        Ville depart = new Ville("Paris");
        Ville arrivee = new Ville("Lyon");
        ServiceFerroviaire service = new ServiceFerroviaire(
                LocalDate.of(2026, 5, 15),
                LocalTime.of(9, 0),
                train,
                depart,
                arrivee,
                50.0
        );
        service.setServiceId(currentServiceId);

        Billet billet = new Billet("CODE-DEMO", BigDecimal.TEN, client);
        billet.setTicketId(UUID.randomUUID());
        billet.setEtat(TicketStatus.DISPONIBLE);
        billet.setDateEmission(LocalDateTime.of(2026, 5, 15, 8, 30));

        SegmentBillet segment = new SegmentBillet(1, service);
        segment.setBillet(billet);
        segment.setEtatSegment(SegmentStatus.PREVU);
        segment.setOrdreDepartValide(1);
        segment.setOrdreArriveeValide(2);
        billet.setSegments(List.of(segment));
        return billet;
    }

    private ServiceCheckpoint checkpointFor(SegmentBillet segment, UUID checkpointId, int order) {
        ServiceCheckpoint checkpoint = new ServiceCheckpoint();
        checkpoint.setCheckpointId(checkpointId);
        checkpoint.setService(segment.getService());
        checkpoint.setVille(new Ville("Checkpoint-" + order));
        checkpoint.setOrdre(order);
        return checkpoint;
    }
}
