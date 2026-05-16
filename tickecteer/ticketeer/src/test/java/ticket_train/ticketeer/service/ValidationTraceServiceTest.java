package ticket_train.ticketeer.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceCheckpoint;
import ticket_train.ticketeer.model.Validation;
import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.repository.ValidationRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ValidationTraceServiceTest {

    private final ValidationRepository validationRepository = mock(ValidationRepository.class);
    private final ValidationTraceService validationTraceService = new ValidationTraceService(validationRepository);

    @Test
    void savesUnknownTicketAttemptWithoutRawCode() {
        Controleur controleur = new Controleur("ctrl", "hash", "Nom", "Prenom");
        UUID serviceId = UUID.randomUUID();
        UUID checkpointId = UUID.randomUUID();

        validationTraceService.saveAttemptTrace(
                controleur,
                "RAW-CODE-123",
                serviceId,
                checkpointId,
                null,
                ValidationResult.INVALID,
                ValidationMotif.BILLET_INCONNU,
                null
        );

        Validation trace = captureSavedTrace();
        assertNull(trace.getSegment());
        assertEquals(ValidationResult.INVALID, trace.getResultat());
        assertEquals(ValidationMotif.BILLET_INCONNU, trace.getMotif());
        assertEquals(serviceId, trace.getRequestedServiceId());
        assertEquals(checkpointId, trace.getRequestedCheckpointId());
        assertNotNull(trace.getSubmittedCodeHash());
        assertEquals(64, trace.getSubmittedCodeHash().length());
        assertNotEquals("RAW-CODE-123", trace.getSubmittedCodeHash());
    }

    @Test
    void preservesValidResultMotifSegmentAndCheckpointOrder() {
        Controleur controleur = new Controleur("ctrl", "hash", "Nom", "Prenom");
        SegmentBillet segment = new SegmentBillet();
        ServiceCheckpoint checkpoint = new ServiceCheckpoint();
        checkpoint.setOrdre(3);

        validationTraceService.saveAttemptTrace(
                controleur,
                "VALID-CODE",
                UUID.randomUUID(),
                UUID.randomUUID(),
                segment,
                ValidationResult.VALID,
                ValidationMotif.OK,
                checkpoint
        );

        Validation trace = captureSavedTrace();
        assertEquals(segment, trace.getSegment());
        assertEquals(3, trace.getCheckpointOrder());
        assertEquals(ValidationResult.VALID, trace.getResultat());
        assertEquals(ValidationMotif.OK, trace.getMotif());
    }

    @Test
    void tracePersistenceFailureDoesNotBreakValidationFlow() {
        doThrow(new IllegalStateException("legacy schema refuses null segment"))
                .when(validationRepository)
                .flush();

        assertDoesNotThrow(() -> validationTraceService.saveAttemptTrace(
                new Controleur("ctrl", "hash", "Nom", "Prenom"),
                "SNCF-20260514-ZZ-000000",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                ValidationResult.INVALID,
                ValidationMotif.BILLET_INCONNU,
                null
        ));
    }

    private Validation captureSavedTrace() {
        ArgumentCaptor<Validation> captor = ArgumentCaptor.forClass(Validation.class);
        verify(validationRepository).save(captor.capture());
        return captor.getValue();
    }
}
