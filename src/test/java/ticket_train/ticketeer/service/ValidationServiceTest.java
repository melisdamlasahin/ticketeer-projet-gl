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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private BilletRepository billetRepository;
    @Mock
    private SegmentBilletRepository segmentBilletRepository;
    @Mock
    private ServiceCheckpointRepository serviceCheckpointRepository;
    @Mock
    private ValidationRepository validationRepository;
    @Mock
    private ServiceFerroviaireRepository serviceFerroviaireRepository;
    @Mock
    private SignedQrService signedQrService;
    @Mock
    private FraudDetectionService fraudDetectionService;
    @Mock
    private ValidationTraceService validationTraceService;

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
                validationTraceService
        );
        controleur = new Controleur("ctrl", "hash", "Nom", "Prenom");
        serviceId = UUID.randomUUID();
    }

    @Test
    void returnsBilletInconnuForUnknownTicket() {
        when(signedQrService.parseAndVerify("UNKNOWN"))
                .thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptique("UNKNOWN")).thenReturn(Optional.empty());

        ValidationResponse response = validationService.validerBillet(new ValidationRequest("UNKNOWN", serviceId), controleur);

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.BILLET_INCONNU, response.getMotif());
    }

    @Test
    void returnsDejaValideForAlreadyValidatedSegment() {
        Billet billet = buildBilletWithSegment(SegmentStatus.VALIDE, serviceId, 1, 2);
        when(signedQrService.parseAndVerify("CODE")).thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptique("CODE")).thenReturn(Optional.of(billet));
        when(fraudDetectionService.detectValidationIssue(any())).thenReturn(ValidationMotif.DEJA_VALIDE);

        ValidationResponse response = validationService.validerBillet(new ValidationRequest("CODE", serviceId), controleur);

        assertEquals(ValidationMotif.DEJA_VALIDE, response.getMotif());
        verify(validationTraceService).saveTrace(any(), any(), any(), any());
        verify(segmentBilletRepository, never()).save(any());
    }

    @Test
    void returnsNonConformeServiceForWrongService() {
        Billet billet = buildBilletWithSegment(SegmentStatus.PREVU, UUID.randomUUID(), 1, 2);
        when(signedQrService.parseAndVerify("CODE")).thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptique("CODE")).thenReturn(Optional.of(billet));

        ValidationResponse response = validationService.validerBillet(new ValidationRequest("CODE", serviceId), controleur);

        assertEquals(ValidationMotif.NON_CONFORME_SERVICE, response.getMotif());
    }

    @Test
    void returnsCodeIllisibleForMalformedQr() {
        when(signedQrService.parseAndVerify("BADQR"))
                .thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.MALFORMED, null));

        ValidationResponse response = validationService.validerBillet(new ValidationRequest("BADQR", serviceId), controleur);

        assertEquals(ValidationMotif.CODE_ILLISIBLE, response.getMotif());
    }

    @Test
    void returnsQrSignatureInvalideForTamperedSignedQr() {
        when(signedQrService.parseAndVerify("TAMPERED"))
                .thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.INVALID_SIGNATURE, null));

        ValidationResponse response = validationService.validerBillet(new ValidationRequest("TAMPERED", serviceId), controleur);

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.QR_SIGNATURE_INVALIDE, response.getMotif());
        verify(billetRepository, never()).findByCodeOptique(any());
    }

    @Test
    void returnsValidationImpossibleTemporairementWhenRepositoryFails() {
        when(signedQrService.parseAndVerify("CODE")).thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptique("CODE")).thenThrow(new DataAccessResourceFailureException("db down"));

        ValidationResponse response = validationService.validerBillet(new ValidationRequest("CODE", serviceId), controleur);

        assertEquals(ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT, response.getMotif());
    }

    @Test
    void validatesTicketAndUpdatesState() {
        Billet billet = buildBilletWithSegment(SegmentStatus.PREVU, serviceId, 1, 2);
        UUID checkpointId = UUID.randomUUID();
        when(serviceCheckpointRepository.findById(checkpointId))
                .thenReturn(Optional.of(buildCheckpoint((SegmentBillet) billet.getSegments().get(0), checkpointId, "Paris", 1)));
        when(signedQrService.parseAndVerify("CODE")).thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptique("CODE")).thenReturn(Optional.of(billet));
        when(fraudDetectionService.detectValidationIssue(any())).thenReturn(null);

        ValidationResponse response = validationService.validerBillet(new ValidationRequest("CODE", serviceId, checkpointId), controleur);

        assertEquals(ValidationResult.VALID, response.getResultat());
        assertEquals(ValidationMotif.OK, response.getMotif());
        assertEquals(SegmentStatus.VALIDE, billet.getSegments().get(0).getEtatSegment());
        assertEquals(TicketStatus.EN_UTILISATION, billet.getEtat());
        verify(validationTraceService).saveTrace(any(), any(), any(), any());
    }

    @Test
    void rejectsValidationBeyondDestinationCheckpoint() {
        Billet billet = buildBilletWithSegment(SegmentStatus.PREVU, serviceId, 1, 2);
        UUID checkpointId = UUID.randomUUID();
        when(serviceCheckpointRepository.findById(checkpointId))
                .thenReturn(Optional.of(buildCheckpoint((SegmentBillet) billet.getSegments().get(0), checkpointId, "Marseille", 3)));
        when(signedQrService.parseAndVerify("CODE")).thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptique("CODE")).thenReturn(Optional.of(billet));
        when(fraudDetectionService.detectValidationIssue(any())).thenReturn(null);

        ValidationResponse response = validationService.validerBillet(new ValidationRequest("CODE", serviceId, checkpointId), controleur);

        assertEquals(ValidationResult.INVALID, response.getResultat());
        assertEquals(ValidationMotif.HORS_PARCOURS_AUTORISE, response.getMotif());
        assertEquals(SegmentStatus.INVALIDE, billet.getSegments().get(0).getEtatSegment());
    }

    @Test
    void completesSegmentAtArrivalCheckpoint() {
        Billet billet = buildBilletWithSegment(SegmentStatus.PREVU, serviceId, 1, 2);
        UUID checkpointId = UUID.randomUUID();
        when(serviceCheckpointRepository.findById(checkpointId))
                .thenReturn(Optional.of(buildCheckpoint((SegmentBillet) billet.getSegments().get(0), checkpointId, "Lyon", 2)));
        when(signedQrService.parseAndVerify("CODE")).thenReturn(new SignedQrService.ParseResult(SignedQrService.ParseStatus.NOT_SIGNED, null));
        when(billetRepository.findByCodeOptique("CODE")).thenReturn(Optional.of(billet));
        when(fraudDetectionService.detectValidationIssue(any())).thenReturn(null);

        ValidationResponse response = validationService.validerBillet(new ValidationRequest("CODE", serviceId, checkpointId), controleur);

        assertEquals(ValidationResult.VALID, response.getResultat());
        assertEquals(SegmentStatus.TERMINE, billet.getSegments().get(0).getEtatSegment());
        assertEquals(TicketStatus.TERMINE, billet.getEtat());
    }

    private Billet buildBilletWithSegment(SegmentStatus status, UUID currentServiceId, int startOrder, int endOrder) {
        Client client = new Client("Jean", "Dupont", "jean@test", "hash", "photo");
        client.setClientId(UUID.randomUUID());

        Train train = new Train("T1", "TGV Test");
        Ville depart = new Ville("Paris");
        Ville arrivee = new Ville("Lyon");
        ServiceFerroviaire service = new ServiceFerroviaire(LocalDate.of(2026, 3, 18), train, depart, arrivee, 50.0);
        service.setServiceId(currentServiceId);

        Billet billet = new Billet("CODE", BigDecimal.TEN, client);
        billet.setEtat(TicketStatus.DISPONIBLE);
        billet.setTicketId(UUID.randomUUID());

        SegmentBillet segment = new SegmentBillet(1, service);
        segment.setBillet(billet);
        segment.setEtatSegment(status);
        segment.setOrdreDepartValide(startOrder);
        segment.setOrdreArriveeValide(endOrder);
        billet.setSegments(List.of(segment));
        return billet;
    }

    private ServiceCheckpoint buildCheckpoint(SegmentBillet segment, UUID checkpointId, String cityName, int order) {
        ServiceCheckpoint checkpoint = new ServiceCheckpoint();
        checkpoint.setCheckpointId(checkpointId);
        checkpoint.setService(segment.getService());
        checkpoint.setVille(new Ville(cityName));
        checkpoint.setOrdre(order);
        return checkpoint;
    }
}
