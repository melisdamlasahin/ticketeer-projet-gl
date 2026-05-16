package ticket_train.ticketeer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ticket_train.ticketeer.dto.ValidationRequest;
import ticket_train.ticketeer.dto.ValidationResponse;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ValidationConcurrencyIntegrationTest {

    @Autowired private ValidationService validationService;
    @Autowired private ValidationRepository validationRepository;
    @Autowired private TrainRepository trainRepository;
    @Autowired private VilleRepository villeRepository;
    @Autowired private ServiceFerroviaireRepository serviceFerroviaireRepository;
    @Autowired private ServiceCheckpointRepository serviceCheckpointRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ControleurRepository controleurRepository;
    @Autowired private BilletRepository billetRepository;
    @Autowired private PlatformTransactionManager transactionManager;

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
    void simultaneousValidationAcceptsAtMostOnce() throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ValidationIntegrationFixture.FixtureData data = transactionTemplate.execute(status ->
                fixture.createValidationCase("CONCURRENT-" + UUID.randomUUID())
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<ValidationResponse> task = () -> {
                ready.countDown();
                start.await();
                return validationService.validerBillet(new ValidationRequest(
                        data.billet().getCodeOptique(),
                        data.service().getServiceId(),
                        data.departureCheckpoint().getCheckpointId()
                ), data.controleur());
            };

            Future<ValidationResponse> first = executor.submit(task);
            Future<ValidationResponse> second = executor.submit(task);
            ready.await();
            start.countDown();

            List<ValidationResponse> responses = List.of(first.get(), second.get());
            long acceptedResponses = responses.stream()
                    .filter(response -> response.getResultat() == ValidationResult.VALID)
                    .count();
            long duplicateResponses = responses.stream()
                    .filter(response -> response.getMotif() == ValidationMotif.DEJA_VALIDE)
                    .count();

            assertEquals(1, acceptedResponses);
            assertEquals(1, duplicateResponses);
        } finally {
            executor.shutdownNow();
        }

        List<Validation> traces = validationRepository.findTop5BySegmentOrderByTimestampControleDesc(data.segment());
        long acceptedTraces = traces.stream()
                .filter(trace -> trace.getResultat() == ValidationResult.VALID)
                .count();
        assertEquals(1, acceptedTraces);
        assertTrue(traces.stream().anyMatch(trace -> trace.getMotif() == ValidationMotif.DEJA_VALIDE));
    }
}
