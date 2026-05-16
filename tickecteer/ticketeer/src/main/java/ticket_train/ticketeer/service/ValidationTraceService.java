package ticket_train.ticketeer.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceCheckpoint;
import ticket_train.ticketeer.model.Validation;
import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.repository.ValidationRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

@Service
public class ValidationTraceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationTraceService.class);

    private final ValidationRepository validationRepository;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public ValidationTraceService(ValidationRepository validationRepository,
                                  PlatformTransactionManager transactionManager) {
        this.validationRepository = validationRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    ValidationTraceService(ValidationRepository validationRepository) {
        this.validationRepository = validationRepository;
        this.transactionTemplate = null;
    }

    public void saveTrace(Controleur controleur,
                          SegmentBillet segment,
                          ValidationResult resultat,
                          ValidationMotif motif) {
        saveTrace(controleur, segment, resultat, motif, null);
    }

    public void saveTrace(Controleur controleur,
                          SegmentBillet segment,
                          ValidationResult resultat,
                          ValidationMotif motif,
                          ServiceCheckpoint checkpoint) {
        saveBestEffort(new Validation(
                resultat,
                motif,
                controleur,
                segment,
                checkpoint != null ? checkpoint.getOrdre() : null
        ));
    }

    public void saveAttemptTrace(Controleur controleur,
                                 String submittedCode,
                                 UUID requestedServiceId,
                                 UUID requestedCheckpointId,
                                 SegmentBillet segment,
                                 ValidationResult resultat,
                                 ValidationMotif motif,
                                 ServiceCheckpoint checkpoint) {
        saveBestEffort(new Validation(
                resultat,
                motif,
                controleur,
                segment,
                checkpoint != null ? checkpoint.getOrdre() : null,
                hashSubmittedCode(submittedCode),
                requestedServiceId,
                requestedCheckpointId
        ));
    }

    public List<Validation> getRecentTracesForController(Controleur controleur) {
        return validationRepository.findTop10ByControleurOrderByTimestampControleDesc(controleur);
    }

    private String hashSubmittedCode(String submittedCode) {
        if (submittedCode == null || submittedCode.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(submittedCode.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }

    private void saveBestEffort(Validation validation) {
        try {
            if (transactionTemplate == null) {
                persist(validation);
            } else {
                transactionTemplate.executeWithoutResult(status -> persist(validation));
            }
        } catch (RuntimeException ex) {
            LOGGER.warn(
                    "Validation trace could not be persisted: result={} motif={} requestedServiceId={} requestedCheckpointId={}",
                    validation.getResultat(),
                    validation.getMotif(),
                    validation.getRequestedServiceId(),
                    validation.getRequestedCheckpointId()
            );
        }
    }

    private void persist(Validation validation) {
        validationRepository.save(validation);
        validationRepository.flush();
    }
}
