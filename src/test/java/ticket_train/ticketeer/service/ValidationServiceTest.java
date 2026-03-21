package ticket_train.ticketeer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ticket_train.ticketeer.dto.ValidationResponse;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.model.Train;
import ticket_train.ticketeer.model.Validation;
import ticket_train.ticketeer.model.Ville;
import ticket_train.ticketeer.model.enums.SegmentStatus;
import ticket_train.ticketeer.model.enums.TicketStatus;
import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.repository.BilletRepository;
import ticket_train.ticketeer.repository.SegmentBilletRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
    private FraudDetectionService fraudDetectionService;
    @Mock
    private ValidationTraceService validationTraceService;
    @Mock
    private ValidationHubClient validationHubClient;

    @InjectMocks
    private ValidationService validationService;

    private Controleur controleur;
    private Billet billet;
    private SegmentBillet segment;
    private UUID serviceId;

    @BeforeEach
    void setUp() {
        controleur = new Controleur("ctrl-01", "encoded", "Doe", "Jane");
        controleur.setControleurId(UUID.randomUUID());

        Client client = new Client("Dupont", "Jean", "photo.jpg");
        billet = new Billet("CODE-123", BigDecimal.valueOf(45.0), client);
        billet.setTicketId(UUID.randomUUID());
        billet.setEtat(TicketStatus.DISPONIBLE);

        serviceId = UUID.randomUUID();
        ServiceFerroviaire service = new ServiceFerroviaire(
                LocalDate.now(),
                new Train("TGV100", "TGV"),
                new Ville("Paris"),
                new Ville("Lyon"),
                80.0
        );
        service.setServiceId(serviceId);

        segment = new SegmentBillet(1, service);
        segment.setSegmentId(UUID.randomUUID());
        segment.setEtatSegment(SegmentStatus.PREVU);
        segment.setBillet(billet);
        billet.setSegments(List.of(segment));
    }

    @Test
    void shouldReturnCodeIllisibleWhenCodeIsBlank() {
        Validation trace = trace(ValidationResult.INVALID, ValidationMotif.CODE_ILLISIBLE, null);
        when(validationTraceService.saveTrace(any(), any(), any(), any(), any(), any())).thenReturn(trace);

        ValidationResponse response = validationService.validateTicket(" ", serviceId, LocalDateTime.now(), controleur);

        assertThat(response.getResultat()).isEqualTo(ValidationResult.INVALID);
        assertThat(response.getMotif()).isEqualTo(ValidationMotif.CODE_ILLISIBLE);
        verify(billetRepository, never()).findForValidationByCodeOptique(any());
    }

    @Test
    void shouldReturnBilletInconnuWhenTicketDoesNotExist() {
        Validation trace = trace(ValidationResult.INVALID, ValidationMotif.BILLET_INCONNU, null);
        when(billetRepository.findForValidationByCodeOptique("UNKNOWN")).thenReturn(Optional.empty());
        when(validationTraceService.saveTrace(any(), any(), any(), any(), any(), any())).thenReturn(trace);

        ValidationResponse response = validationService.validateTicket("UNKNOWN", serviceId, LocalDateTime.now(), controleur);

        assertThat(response.getMotif()).isEqualTo(ValidationMotif.BILLET_INCONNU);
    }

    @Test
    void shouldReturnDejaValideWhenDuplicateAcceptanceDetected() {
        Validation trace = trace(ValidationResult.INVALID, ValidationMotif.DEJA_VALIDE, segment);
        when(billetRepository.findForValidationByCodeOptique("CODE-123")).thenReturn(Optional.of(billet));
        when(fraudDetectionService.isDuplicateAcceptance(segment)).thenReturn(true);
        when(validationTraceService.saveTrace(any(), any(), any(), any(), any(), any())).thenReturn(trace);

        ValidationResponse response = validationService.validateTicket("CODE-123", serviceId, LocalDateTime.now(), controleur);

        assertThat(response.getMotif()).isEqualTo(ValidationMotif.DEJA_VALIDE);
        verify(validationHubClient, never()).confirmValidation(any(), any());
    }

    @Test
    void shouldReturnNonConformeServiceWhenServiceDoesNotMatch() {
        UUID otherServiceId = UUID.randomUUID();
        Validation trace = trace(ValidationResult.INVALID, ValidationMotif.NON_CONFORME_SERVICE, null);
        when(billetRepository.findForValidationByCodeOptique("CODE-123")).thenReturn(Optional.of(billet));
        when(validationTraceService.saveTrace(any(), any(), any(), any(), any(), any())).thenReturn(trace);

        ValidationResponse response = validationService.validateTicket("CODE-123", otherServiceId, LocalDateTime.now(), controleur);

        assertThat(response.getMotif()).isEqualTo(ValidationMotif.NON_CONFORME_SERVICE);
    }

    @Test
    void shouldReturnTemporaryUnavailableWhenHubFails() {
        Validation trace = trace(ValidationResult.INVALID, ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT, segment);
        when(billetRepository.findForValidationByCodeOptique("CODE-123")).thenReturn(Optional.of(billet));
        when(fraudDetectionService.isDuplicateAcceptance(segment)).thenReturn(false);
        when(fraudDetectionService.canTransitionToValid(segment)).thenReturn(true);
        when(validationTraceService.saveTrace(
                ValidationResult.INVALID,
                ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT,
                "CODE-123",
                serviceId,
                controleur,
                segment
        )).thenReturn(trace);
        org.mockito.Mockito.doThrow(new ValidationUnavailableException("offline"))
                .when(validationHubClient).confirmValidation(billet, segment);

        ValidationResponse response = validationService.validateTicket("CODE-123", serviceId, LocalDateTime.now(), controleur);

        assertThat(response.getMotif()).isEqualTo(ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT);
        assertThat(segment.getEtatSegment()).isEqualTo(SegmentStatus.PREVU);
    }

    @Test
    void shouldValidateTicketAndUpdateSegmentState() {
        Validation trace = trace(ValidationResult.VALID, ValidationMotif.OK, segment);
        when(billetRepository.findForValidationByCodeOptique("CODE-123")).thenReturn(Optional.of(billet));
        when(fraudDetectionService.isDuplicateAcceptance(segment)).thenReturn(false);
        when(fraudDetectionService.canTransitionToValid(segment)).thenReturn(true);
        doNothing().when(validationHubClient).confirmValidation(billet, segment);
        when(validationTraceService.saveTrace(any(), any(), any(), any(), any(), any())).thenReturn(trace);
        when(segmentBilletRepository.save(segment)).thenReturn(segment);

        ValidationResponse response = validationService.validateTicket("CODE-123", serviceId, LocalDateTime.now(), controleur);

        assertThat(response.getResultat()).isEqualTo(ValidationResult.VALID);
        assertThat(segment.getEtatSegment()).isEqualTo(SegmentStatus.VALIDE);
        assertThat(billet.getEtat()).isEqualTo(TicketStatus.TERMINE);

        ArgumentCaptor<SegmentBillet> captor = ArgumentCaptor.forClass(SegmentBillet.class);
        verify(segmentBilletRepository).save(captor.capture());
        assertThat(captor.getValue().getEtatSegment()).isEqualTo(SegmentStatus.VALIDE);
    }

    private Validation trace(ValidationResult result, ValidationMotif motif, SegmentBillet tracedSegment) {
        Validation validation = new Validation(
                result,
                motif,
                "CODE-123",
                serviceId,
                controleur,
                tracedSegment
        );
        validation.setValidationId(UUID.randomUUID());
        return validation;
    }
}
