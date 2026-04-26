package ticket_train.ticketeer.service;

import org.springframework.stereotype.Service;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.enums.SegmentStatus;
import ticket_train.ticketeer.model.enums.ValidationMotif;

@Service
public class FraudDetectionService {

    public ValidationMotif detectValidationIssue(SegmentBillet segmentBillet) {
        if (segmentBillet == null) {
            return ValidationMotif.NON_CONFORME_SERVICE;
        }
        if (segmentBillet.getEtatSegment() == SegmentStatus.TERMINE) {
            return ValidationMotif.TRAJET_TERMINE;
        }
        if (segmentBillet.getEtatSegment() == SegmentStatus.INVALIDE) {
            return ValidationMotif.HORS_PARCOURS_AUTORISE;
        }
        return null;
    }
}
