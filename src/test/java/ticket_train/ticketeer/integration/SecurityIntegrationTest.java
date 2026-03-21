package ticket_train.ticketeer.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.repository.ControleurRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:security-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class SecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ControleurRepository controleurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        controleurRepository.deleteAll();
        controleurRepository.save(new Controleur(
                "controleur1",
                passwordEncoder.encode("password"),
                "Doe",
                "Jane"
        ));
    }

    @Test
    void shouldRejectAnonymousValidationRequest() throws Exception {
        mockMvc.perform(post("/api/validations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedControllerAccess() throws Exception {
        mockMvc.perform(get("/api/control-unit/me")
                        .with(httpBasic("controleur1", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("controleur1"));
    }
}
