package ticket_train.ticketeer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ticket_train.ticketeer.dto.ValidationResponse;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.Validation;
import ticket_train.ticketeer.model.enums.SegmentStatus;
import ticket_train.ticketeer.model.enums.TicketStatus;
import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.repository.BilletRepository;
import ticket_train.ticketeer.repository.SegmentBilletRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ValidationService {
    private final BilletRepository billetRepository;
    private final SegmentBilletRepository segmentBilletRepository;
    private final FraudDetectionService fraudDetectionService;
    private final ValidationTraceService validationTraceService;
    private final ValidationHubClient validationHubClient;

    public ValidationService(
            BilletRepository billetRepository,
            SegmentBilletRepository segmentBilletRepository,
            FraudDetectionService fraudDetectionService,
            ValidationTraceService validationTraceService,
            ValidationHubClient validationHubClient
    ) {
        this.billetRepository = billetRepository;
        this.segmentBilletRepository = segmentBilletRepository;
        this.fraudDetectionService = fraudDetectionService;
        this.validationTraceService = validationTraceService;
        this.validationHubClient = validationHubClient;
    }

    @Transactional
    public ValidationResponse validateTicket(
            String codeOptique,
            UUID serviceId,
            LocalDateTime timestamp,
            Controleur controleur
    ) {
        LocalDateTime effectiveTimestamp = timestamp != null ? timestamp : LocalDateTime.now();

        if (codeOptique == null || codeOptique.isBlank()) {
            Validation trace = validationTraceService.saveTrace(
                    ValidationResult.INVALID,
                    ValidationMotif.CODE_ILLISIBLE,
                    codeOptique == null ? "" : codeOptique,
                    serviceId,
                    controleur,
                    null
            );
            return toResponse(trace, null, null, controleur, effectiveTimestamp);
        }

        Optional<Billet> billetOptional = billetRepository.findForValidationByCodeOptique(codeOptique.trim());
        if (billetOptional.isEmpty()) {
            Validation trace = validationTraceService.saveTrace(
                    ValidationResult.INVALID,
                    ValidationMotif.BILLET_INCONNU,
                    codeOptique.trim(),
                    serviceId,
                    controleur,
                    null
            );
            return toResponse(trace, null, null, controleur, effectiveTimestamp);
        }

        Billet billet = billetOptional.get();
        Optional<SegmentBillet> segmentOptional = billet.getSegments().stream()
                .filter(segment -> segment.getService() != null)
                .filter(segment -> Objects.equals(segment.getService().getServiceId(), serviceId))
                .findFirst();

        if (segmentOptional.isEmpty()) {
            Validation trace = validationTraceService.saveTrace(
                    ValidationResult.INVALID,
                    ValidationMotif.NON_CONFORME_SERVICE,
                    codeOptique.trim(),
                    serviceId,
                    controleur,
                    null
            );
            return toResponse(trace, billet, null, controleur, effectiveTimestamp);
        }

        SegmentBillet segment = segmentOptional.get();
        if (fraudDetectionService.isDuplicateAcceptance(segment)) {
            Validation trace = validationTraceService.saveTrace(
                    ValidationResult.INVALID,
                    ValidationMotif.DEJA_VALIDE,
                    codeOptique.trim(),
                    serviceId,
                    controleur,
                    segment
            );
            return toResponse(trace, billet, segment, controleur, effectiveTimestamp);
        }

        if (!fraudDetectionService.canTransitionToValid(segment)) {
            Validation trace = validationTraceService.saveTrace(
                    ValidationResult.INVALID,
                    ValidationMotif.NON_CONFORME_SERVICE,
                    codeOptique.trim(),
                    serviceId,
                    controleur,
                    segment
            );
            return toResponse(trace, billet, segment, controleur, effectiveTimestamp);
        }

        try {
            validationHubClient.confirmValidation(billet, segment);
        } catch (ValidationUnavailableException exception) {
            Validation trace = validationTraceService.saveTrace(
                    ValidationResult.INVALID,
                    ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT,
                    codeOptique.trim(),
                    serviceId,
                    controleur,
                    segment
            );
            return toResponse(trace, billet, segment, controleur, effectiveTimestamp);
        }

        segment.setEtatSegment(SegmentStatus.VALIDE);
        segmentBilletRepository.save(segment);
        updateTicketState(billet);

        Validation trace = validationTraceService.saveTrace(
                ValidationResult.VALID,
                ValidationMotif.OK,
                codeOptique.trim(),
                serviceId,
                controleur,
                segment
        );
        return toResponse(trace, billet, segment, controleur, effectiveTimestamp);
    }

    private void updateTicketState(Billet billet) {
        boolean allValidated = billet.getSegments().stream()
                .allMatch(segment -> segment.getEtatSegment() == SegmentStatus.VALIDE);

        billet.setEtat(allValidated ? TicketStatus.TERMINE : TicketStatus.EN_UTILISATION);
        billet.getSegments().stream()
                .max(Comparator.comparingInt(SegmentBillet::getOrdre))
                .ifPresent(lastSegment -> {
                    if (allValidated && lastSegment.getEtatSegment() == SegmentStatus.VALIDE) {
                        billet.setEtat(TicketStatus.TERMINE);
                    }
                });
    }

    private ValidationResponse toResponse(
            Validation validation,
            Billet billet,
            SegmentBillet segment,
            Controleur controleur,
            LocalDateTime timestamp
    ) {
        ValidationResponse response = new ValidationResponse();
        response.setValidationId(validation.getValidationId());
        response.setTicketId(billet != null ? billet.getTicketId() : null);
        response.setSegmentId(segment != null ? segment.getSegmentId() : null);
        response.setServiceId(validation.getServiceId());
        response.setControllerLogin(controleur.getLogin());
        response.setResultat(validation.getResultat());
        response.setMotif(validation.getMotif());
        response.setTimestampControle(validation.getTimestampControle() != null ? validation.getTimestampControle() : timestamp);
        return response;
    }
}
