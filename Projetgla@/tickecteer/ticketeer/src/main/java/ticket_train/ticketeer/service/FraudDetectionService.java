package ticket_train.ticketeer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceCheckpoint;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.model.Validation;
import ticket_train.ticketeer.model.enums.SegmentStatus;
import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.repository.ValidationRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudDetectionService {

    private static final int MAX_CONTROLLER_VALIDATIONS_PER_MINUTE = 30;
    private static final int MAX_CONTROLLER_INVALIDATIONS_PER_TWO_MINUTES = 12;
    private static final int SEGMENT_REPLAY_WINDOW_MINUTES = 180;
    private static final int CLIENT_ANOMALY_WINDOW_MINUTES = 30;
    private static final int MIN_MINUTES_PER_CHECKPOINT_HOP = 10;
    private static final int CHECKPOINT_TIME_GRACE_MINUTES = 2;

    private final ValidationRepository validationRepository;
    private final Clock clock;

    @Autowired
    public FraudDetectionService(ValidationRepository validationRepository) {
        this(validationRepository, Clock.systemDefaultZone());
    }

    FraudDetectionService(ValidationRepository validationRepository, Clock clock) {
        this.validationRepository = validationRepository;
        this.clock = clock;
    }

    public ValidationMotif detectValidationIssue(SegmentBillet segmentBillet,
                                                 Controleur controleur,
                                                 ServiceCheckpoint checkpoint) {
        if (segmentBillet == null) {
            return ValidationMotif.NON_CONFORME_SERVICE;
        }
        if (segmentBillet.getEtatSegment() == SegmentStatus.TERMINE) {
            return ValidationMotif.TRAJET_TERMINE;
        }
        if (segmentBillet.getEtatSegment() == SegmentStatus.INVALIDE) {
            return ValidationMotif.HORS_PARCOURS_AUTORISE;
        }
        if (controleur != null && isControllerVelocitySuspicious(controleur)) {
            return ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT;
        }
        if (checkpoint != null) {
            ValidationMotif replayOrRouteIssue = detectReplayOrBacktracking(segmentBillet, checkpoint);
            if (replayOrRouteIssue != null) {
                return replayOrRouteIssue;
            }
        }
        if (hasCrossServiceClientAnomaly(segmentBillet)) {
            return ValidationMotif.NON_CONFORME_SERVICE;
        }
        return null;
    }

    private boolean isControllerVelocitySuspicious(Controleur controleur) {
        LocalDateTime now = LocalDateTime.now(clock);
        long recentCount = validationRepository.countByControleurAndTimestampControleAfter(
                controleur,
                now.minusMinutes(1)
        );
        if (recentCount > MAX_CONTROLLER_VALIDATIONS_PER_MINUTE) {
            return true;
        }
        long invalidBurst = validationRepository.countByControleurAndResultatAndTimestampControleAfter(
                controleur,
                ValidationResult.INVALID,
                now.minusMinutes(2)
        );
        return invalidBurst > MAX_CONTROLLER_INVALIDATIONS_PER_TWO_MINUTES;
    }

    private ValidationMotif detectReplayOrBacktracking(SegmentBillet segmentBillet, ServiceCheckpoint checkpoint) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusMinutes(SEGMENT_REPLAY_WINDOW_MINUTES);
        List<Validation> traces = validationRepository.findTop5BySegmentOrderByTimestampControleDesc(segmentBillet);
        for (Validation trace : traces) {
            if (trace.getTimestampControle() == null || trace.getTimestampControle().isBefore(cutoff)) {
                continue;
            }
            if (trace.getResultat() != ValidationResult.VALID) {
                continue;
            }
            Integer previousCheckpointOrder = trace.getCheckpointOrder();
            if (previousCheckpointOrder == null) {
                return ValidationMotif.DEJA_VALIDE;
            }
            if (previousCheckpointOrder.equals(checkpoint.getOrdre())) {
                return ValidationMotif.DEJA_VALIDE;
            }
            if (previousCheckpointOrder > checkpoint.getOrdre()) {
                return ValidationMotif.HORS_PARCOURS_AUTORISE;
            }
            long minutesSinceTrace = Duration.between(trace.getTimestampControle(), now).toMinutes();
            long minimumExpectedMinutes = (long) (checkpoint.getOrdre() - previousCheckpointOrder) * MIN_MINUTES_PER_CHECKPOINT_HOP;
            if (previousCheckpointOrder < checkpoint.getOrdre()
                    && minutesSinceTrace + CHECKPOINT_TIME_GRACE_MINUTES < minimumExpectedMinutes) {
                return ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT;
            }
        }
        return null;
    }

    private boolean hasCrossServiceClientAnomaly(SegmentBillet currentSegment) {
        if (currentSegment.getBillet() == null || currentSegment.getBillet().getClient() == null) {
            return false;
        }
        LocalDateTime cutoff = LocalDateTime.now(clock).minusMinutes(CLIENT_ANOMALY_WINDOW_MINUTES);
        List<Validation> traces = validationRepository.findTop10BySegment_Billet_Client_ClientIdOrderByTimestampControleDesc(
                currentSegment.getBillet().getClient().getClientId()
        );
        ServiceFerroviaire currentService = currentSegment.getService();
        for (Validation trace : traces) {
            if (trace.getTimestampControle() == null || trace.getTimestampControle().isBefore(cutoff)) {
                continue;
            }
            if (trace.getResultat() != ValidationResult.VALID || trace.getSegment() == null) {
                continue;
            }
            SegmentBillet tracedSegment = trace.getSegment();
            if (tracedSegment.getSegmentId() != null && tracedSegment.getSegmentId().equals(currentSegment.getSegmentId())) {
                continue;
            }
            ServiceFerroviaire tracedService = tracedSegment.getService();
            if (tracedService == null || currentService == null) {
                continue;
            }
            boolean sameTravelDate = tracedService.getDateTrajet() != null
                    && tracedService.getDateTrajet().equals(currentService.getDateTrajet());
            boolean sameService = tracedService.getServiceId() != null
                    && tracedService.getServiceId().equals(currentService.getServiceId());
            boolean sharesEndpoint = tracedService.getVilleDepart().getNom().equalsIgnoreCase(currentService.getVilleDepart().getNom())
                    || tracedService.getVilleDepart().getNom().equalsIgnoreCase(currentService.getVilleArrivee().getNom())
                    || tracedService.getVilleArrivee().getNom().equalsIgnoreCase(currentService.getVilleDepart().getNom())
                    || tracedService.getVilleArrivee().getNom().equalsIgnoreCase(currentService.getVilleArrivee().getNom());
            if (sameTravelDate && !sameService && !sharesEndpoint) {
                return true;
            }
        }
        return false;
    }
}
