package ticket_train.ticketeer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceCheckpoint;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.model.Train;
import ticket_train.ticketeer.model.Validation;
import ticket_train.ticketeer.model.Ville;
import ticket_train.ticketeer.model.enums.SegmentStatus;
import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.repository.ValidationRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 9, 10, 30);

    @Mock
    private ValidationRepository validationRepository;

    private FraudDetectionService fraudDetectionService;
    private Controleur controleur;
    private SegmentBillet segment;
    private ServiceCheckpoint checkpoint;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                FIXED_NOW.toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
        );
        fraudDetectionService = new FraudDetectionService(validationRepository, fixedClock);
        controleur = new Controleur("ctrl", "hash", "Nom", "Prenom");
        segment = buildSegment("Paris", "Lyon", LocalDate.of(2026, 5, 9));
        checkpoint = new ServiceCheckpoint();
        checkpoint.setOrdre(2);
        checkpoint.setService(segment.getService());
        checkpoint.setVille(segment.getService().getVilleArrivee());
    }

    @Test
    void rejectsReplayAtSameCheckpoint() {
        when(validationRepository.countByControleurAndTimestampControleAfter(any(), any())).thenReturn(0L);
        when(validationRepository.countByControleurAndResultatAndTimestampControleAfter(any(), any(), any())).thenReturn(0L);
        Validation replay = new Validation(ValidationResult.VALID, ValidationMotif.OK, controleur, segment, 2);
        replay.setTimestampControle(FIXED_NOW.minusMinutes(5));
        when(validationRepository.findTop5BySegmentOrderByTimestampControleDesc(segment)).thenReturn(List.of(replay));

        ValidationMotif motif = fraudDetectionService.detectValidationIssue(segment, controleur, checkpoint);

        assertEquals(ValidationMotif.DEJA_VALIDE, motif);
    }

    @Test
    void rejectsBacktrackingToEarlierCheckpoint() {
        when(validationRepository.countByControleurAndTimestampControleAfter(any(), any())).thenReturn(0L);
        when(validationRepository.countByControleurAndResultatAndTimestampControleAfter(any(), any(), any())).thenReturn(0L);
        Validation replay = new Validation(ValidationResult.VALID, ValidationMotif.OK, controleur, segment, 3);
        replay.setTimestampControle(FIXED_NOW.minusMinutes(5));
        when(validationRepository.findTop5BySegmentOrderByTimestampControleDesc(segment)).thenReturn(List.of(replay));

        ValidationMotif motif = fraudDetectionService.detectValidationIssue(segment, controleur, checkpoint);

        assertEquals(ValidationMotif.HORS_PARCOURS_AUTORISE, motif);
    }

    @Test
    void flagsControllerVelocitySpike() {
        when(validationRepository.countByControleurAndTimestampControleAfter(eq(controleur), any())).thenReturn(31L);

        ValidationMotif motif = fraudDetectionService.detectValidationIssue(segment, controleur, checkpoint);

        assertEquals(ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT, motif);
    }

    @Test
    void flagsCrossServiceClientAnomaly() {
        when(validationRepository.countByControleurAndTimestampControleAfter(any(), any())).thenReturn(0L);
        when(validationRepository.countByControleurAndResultatAndTimestampControleAfter(any(), any(), any())).thenReturn(0L);
        when(validationRepository.findTop5BySegmentOrderByTimestampControleDesc(any())).thenReturn(List.of());
        SegmentBillet otherSegment = buildSegment("Bordeaux", "Toulouse", LocalDate.of(2026, 5, 9));
        otherSegment.getBillet().setClient(segment.getBillet().getClient());
        Validation otherValidation = new Validation(ValidationResult.VALID, ValidationMotif.OK, controleur, otherSegment, 1);
        otherValidation.setTimestampControle(FIXED_NOW.minusMinutes(10));
        when(validationRepository.findTop10BySegment_Billet_Client_ClientIdOrderByTimestampControleDesc(
                segment.getBillet().getClient().getClientId()
        )).thenReturn(List.of(otherValidation));

        ValidationMotif motif = fraudDetectionService.detectValidationIssue(segment, controleur, checkpoint);

        assertEquals(ValidationMotif.NON_CONFORME_SERVICE, motif);
    }

    @Test
    void flagsImpossibleCheckpointJumpTooFast() {
        when(validationRepository.countByControleurAndTimestampControleAfter(any(), any())).thenReturn(0L);
        when(validationRepository.countByControleurAndResultatAndTimestampControleAfter(any(), any(), any())).thenReturn(0L);
        Validation priorValidation = new Validation(ValidationResult.VALID, ValidationMotif.OK, controleur, segment, 1);
        priorValidation.setTimestampControle(LocalDateTime.of(2026, 5, 9, 10, 25));
        checkpoint.setOrdre(4);
        when(validationRepository.findTop5BySegmentOrderByTimestampControleDesc(segment)).thenReturn(List.of(priorValidation));

        ValidationMotif motif = fraudDetectionService.detectValidationIssue(segment, controleur, checkpoint);

        assertEquals(ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT, motif);
    }

    @Test
    void returnsNullForCleanValidation() {
        when(validationRepository.countByControleurAndTimestampControleAfter(any(), any())).thenReturn(0L);
        when(validationRepository.countByControleurAndResultatAndTimestampControleAfter(any(), any(), any())).thenReturn(0L);
        when(validationRepository.findTop5BySegmentOrderByTimestampControleDesc(any())).thenReturn(List.of());
        when(validationRepository.findTop10BySegment_Billet_Client_ClientIdOrderByTimestampControleDesc(any())).thenReturn(List.of());
        ValidationMotif motif = fraudDetectionService.detectValidationIssue(segment, controleur, checkpoint);

        assertNull(motif);
    }

    private SegmentBillet buildSegment(String departureCity, String arrivalCity, LocalDate date) {
        Client client = new Client("Jean", "Dupont", "jean@test", "hash", "photo");
        client.setClientId(UUID.randomUUID());

        Billet billet = new Billet("TICK-TEST", BigDecimal.TEN, client);
        billet.setTicketId(UUID.randomUUID());

        Train train = new Train("T1", "TGV Test");
        ServiceFerroviaire service = new ServiceFerroviaire(date, LocalTime.of(10, 0), train, new Ville(departureCity), new Ville(arrivalCity), 50.0);
        service.setServiceId(UUID.randomUUID());

        SegmentBillet built = new SegmentBillet(1, service);
        built.setSegmentId(UUID.randomUUID());
        built.setBillet(billet);
        built.setEtatSegment(SegmentStatus.PREVU);
        billet.setSegments(List.of(built));
        return built;
    }
}
