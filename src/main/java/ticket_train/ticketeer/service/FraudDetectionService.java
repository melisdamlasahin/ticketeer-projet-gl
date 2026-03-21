package ticket_train.ticketeer.service;

import org.springframework.stereotype.Service;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.enums.SegmentStatus;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.repository.ValidationRepository;

@Service
public class FraudDetectionService {
    private final ValidationRepository validationRepository;

    public FraudDetectionService(ValidationRepository validationRepository) {
        this.validationRepository = validationRepository;
    }

    public boolean isDuplicateAcceptance(SegmentBillet segment) {
        return segment.getEtatSegment() == SegmentStatus.VALIDE
                || validationRepository.existsBySegmentSegmentIdAndResultat(
                        segment.getSegmentId(),
                        ValidationResult.VALID
                );
    }

    public boolean canTransitionToValid(SegmentBillet segment) {
        return segment.getEtatSegment() == SegmentStatus.PREVU;
    }
}
