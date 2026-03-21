package ticket_train.ticketeer.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.model.Train;
import ticket_train.ticketeer.model.Ville;
import ticket_train.ticketeer.model.enums.SegmentStatus;
import ticket_train.ticketeer.model.enums.TicketStatus;
import ticket_train.ticketeer.repository.BilletRepository;
import ticket_train.ticketeer.repository.ClientRepository;
import ticket_train.ticketeer.repository.ControleurRepository;
import ticket_train.ticketeer.repository.SegmentBilletRepository;
import ticket_train.ticketeer.repository.ServiceFerroviaireRepository;
import ticket_train.ticketeer.repository.TrainRepository;
import ticket_train.ticketeer.repository.ValidationRepository;
import ticket_train.ticketeer.repository.VilleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:validation-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "validation.hub.available=true"
})
class ValidationApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ControleurRepository controleurRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private TrainRepository trainRepository;
    @Autowired
    private VilleRepository villeRepository;
    @Autowired
    private ServiceFerroviaireRepository serviceFerroviaireRepository;
    @Autowired
    private BilletRepository billetRepository;
    @Autowired
    private SegmentBilletRepository segmentBilletRepository;
    @Autowired
    private ValidationRepository validationRepository;

    private UUID serviceId;

    @BeforeEach
    void setUp() {
        validationRepository.deleteAll();
        segmentBilletRepository.deleteAll();
        billetRepository.deleteAll();
        serviceFerroviaireRepository.deleteAll();
        trainRepository.deleteAll();
        villeRepository.deleteAll();
        clientRepository.deleteAll();
        controleurRepository.deleteAll();

        Controleur controleur = new Controleur("controleur1", passwordEncoder.encode("password"), "Doe", "Jane");
        controleurRepository.save(controleur);

        Client client = clientRepository.save(new Client("Dupont", "Jean", "photo.jpg"));
        Train train = trainRepository.save(new Train("TGV100", "Paris-Lyon"));
        Ville depart = villeRepository.save(new Ville("Paris"));
        Ville arrivee = villeRepository.save(new Ville("Lyon"));

        ServiceFerroviaire service = new ServiceFerroviaire(LocalDate.now(), train, depart, arrivee, 99.0);
        service = serviceFerroviaireRepository.save(service);
        serviceId = service.getServiceId();

        Billet billet = new Billet("VALID-CODE", BigDecimal.valueOf(99.0), client);
        billet.setEtat(TicketStatus.DISPONIBLE);
        billet = billetRepository.save(billet);

        SegmentBillet segment = new SegmentBillet(1, service);
        segment.setBillet(billet);
        segment.setEtatSegment(SegmentStatus.PREVU);
        segmentBilletRepository.save(segment);
        billet.setSegments(List.of(segment));
        billetRepository.save(billet);
    }

    @Test
    void shouldReturnValidDecisionForCorrectTicket() throws Exception {
        String payload = """
                {
                  "codeOptique": "VALID-CODE",
                  "serviceId": "%s"
                }
                """.formatted(serviceId);

        mockMvc.perform(post("/api/validations")
                        .with(httpBasic("controleur1", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultat").value("VALID"))
                .andExpect(jsonPath("$.motif").value("OK"));
    }

    @Test
    void shouldReturnUnknownTicketDecision() throws Exception {
        String payload = """
                {
                  "codeOptique": "MISSING-CODE",
                  "serviceId": "%s"
                }
                """.formatted(serviceId);

        mockMvc.perform(post("/api/validations")
                        .with(httpBasic("controleur1", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultat").value("INVALID"))
                .andExpect(jsonPath("$.motif").value("BILLET_INCONNU"));
    }
}
