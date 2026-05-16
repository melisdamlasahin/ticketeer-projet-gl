package ticket_train.ticketeer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ticket_train.ticketeer.dto.ValidationRequest;
import ticket_train.ticketeer.model.Validation;
import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.repository.BilletRepository;
import ticket_train.ticketeer.repository.ClientRepository;
import ticket_train.ticketeer.repository.ControleurRepository;
import ticket_train.ticketeer.repository.ServiceCheckpointRepository;
import ticket_train.ticketeer.repository.ServiceFerroviaireRepository;
import ticket_train.ticketeer.repository.TrainRepository;
import ticket_train.ticketeer.repository.ValidationRepository;
import ticket_train.ticketeer.repository.VilleRepository;
import ticket_train.ticketeer.support.ValidationIntegrationFixture;

import java.util.Comparator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class ValidationTracePersistenceIntegrationTest {

    @Autowired private ValidationService validationService;
    @Autowired private ValidationRepository validationRepository;
    @Autowired private TrainRepository trainRepository;
    @Autowired private VilleRepository villeRepository;
    @Autowired private ServiceFerroviaireRepository serviceFerroviaireRepository;
    @Autowired private ServiceCheckpointRepository serviceCheckpointRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ControleurRepository controleurRepository;
    @Autowired private BilletRepository billetRepository;

    private ValidationIntegrationFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ValidationIntegrationFixture(
                trainRepository,
                villeRepository,
                serviceFerroviaireRepository,
                serviceCheckpointRepository,
                clientRepository,
                controleurRepository,
                billetRepository
        );
    }

    @Test
    void unknownTicketCreatesTraceWithoutRawCode() {
        ValidationIntegrationFixture.FixtureData data = fixture.createValidationCase("KNOWN-" + UUID.randomUUID());
        String submittedCode = "UNKNOWN-" + UUID.randomUUID();

        validationService.validerBillet(new ValidationRequest(
                submittedCode,
                data.service().getServiceId(),
                data.departureCheckpoint().getCheckpointId()
        ), data.controleur());

        Validation trace = latestTrace();
        assertEquals(ValidationResult.INVALID, trace.getResultat());
        assertEquals(ValidationMotif.BILLET_INCONNU, trace.getMotif());
        assertNull(trace.getSegment());
        assertNotNull(trace.getSubmittedCodeHash());
        assertNotEquals(submittedCode, trace.getSubmittedCodeHash());
    }

    @Test
    void unreadableCodeCreatesTraceWithoutRawCode() {
        ValidationIntegrationFixture.FixtureData data = fixture.createValidationCase("KNOWN-" + UUID.randomUUID());

        validationService.validerBillet(new ValidationRequest(
                "TICKETEERQR.missing-signature",
                data.service().getServiceId(),
                data.departureCheckpoint().getCheckpointId()
        ), data.controleur());

        Validation trace = latestTrace();
        assertEquals(ValidationResult.INVALID, trace.getResultat());
        assertEquals(ValidationMotif.CODE_ILLISIBLE, trace.getMotif());
        assertNull(trace.getSegment());
        assertNotNull(trace.getSubmittedCodeHash());
    }

    @Test
    void wrongServiceCreatesTraceWithRequestedService() {
        ValidationIntegrationFixture.FixtureData data = fixture.createValidationCase("WRONG-SERVICE-" + UUID.randomUUID());
        UUID wrongServiceId = UUID.randomUUID();

        validationService.validerBillet(new ValidationRequest(
                data.billet().getCodeOptique(),
                wrongServiceId,
                data.departureCheckpoint().getCheckpointId()
        ), data.controleur());

        Validation trace = latestTrace();
        assertEquals(ValidationResult.INVALID, trace.getResultat());
        assertEquals(ValidationMotif.NON_CONFORME_SERVICE, trace.getMotif());
        assertEquals(wrongServiceId, trace.getRequestedServiceId());
    }

    private Validation latestTrace() {
        return validationRepository.findAll().stream()
                .max(Comparator.comparing(Validation::getTimestampControle))
                .orElseThrow();
    }
}
