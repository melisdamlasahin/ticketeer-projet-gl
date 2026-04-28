package ticket_train.ticketeer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import ticket_train.ticketeer.dto.mobile.AchatBilletRequest;
import ticket_train.ticketeer.dto.mobile.RegisterRequest;
import ticket_train.ticketeer.dto.mobile.LoginRequest;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MobileApiAuthLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerThenAccessProfileWithIssuedToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setNom("Durand");
        request.setPrenom("Alice");
        request.setEmail("alice.lifecycle@example.com");
        request.setMotDePasse("password123");

        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode auth = objectMapper.readTree(responseBody);
        String token = auth.get("authToken").asText();
        String clientId = auth.get("clientId").asText();

        mockMvc.perform(get("/api/clients/" + clientId + "/profile")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.email").value("alice.lifecycle@example.com"));
    }

    @Test
    void staleOrUnknownTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/clients/00000000-0000-0000-0000-000000000000/profile")
                        .header("X-Auth-Token", "stale-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Auth token invalide"));
    }

    @Test
    void bearerTokenCanAccessProtectedEndpoint() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setNom("Durand");
        request.setPrenom("Alice");
        request.setEmail("alice.bearer@example.com");
        request.setMotDePasse("password123");

        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode auth = objectMapper.readTree(responseBody);
        String token = auth.get("authToken").asText();
        String clientId = auth.get("clientId").asText();

        mockMvc.perform(get("/api/clients/" + clientId + "/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId));
    }

    @Test
    void clientCannotReadAnotherClientProfile() throws Exception {
        String first = register("first.authz@example.com");
        String second = register("second.authz@example.com");

        JsonNode firstAuth = objectMapper.readTree(first);
        JsonNode secondAuth = objectMapper.readTree(second);

        mockMvc.perform(get("/api/clients/" + secondAuth.get("clientId").asText() + "/profile")
                        .header("X-Auth-Token", firstAuth.get("authToken").asText()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));
    }

    @Test
    void clientCannotReadAnotherClientsTicketOrPdf() throws Exception {
        JsonNode ownerAuth = objectMapper.readTree(register("owner.ticket@example.com"));
        JsonNode attackerAuth = objectMapper.readTree(register("attacker.ticket@example.com"));
        String serviceId = firstServiceId();
        String billetId = buyTicket(
                ownerAuth.get("authToken").asText(),
                ownerAuth.get("clientId").asText(),
                serviceId
        );

        mockMvc.perform(get("/api/billets/" + billetId)
                        .header("X-Auth-Token", attackerAuth.get("authToken").asText()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));

        mockMvc.perform(get("/api/billets/" + billetId + "/pdf")
                        .header("X-Auth-Token", attackerAuth.get("authToken").asText()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));
    }

    @Test
    void logoutRevokesCurrentToken() throws Exception {
        String registered = register("logout.authz@example.com");
        JsonNode auth = objectMapper.readTree(registered);
        String token = auth.get("authToken").asText();
        String clientId = auth.get("clientId").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/clients/" + clientId + "/profile")
                        .header("X-Auth-Token", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Auth token invalide"));
    }

    @Test
    void repeatedInvalidLoginsAreRateLimited() throws Exception {
        register("ratelimit.authz@example.com");

        LoginRequest request = new LoginRequest();
        request.setEmail("ratelimit.authz@example.com");
        request.setMotDePasse("wrong-password");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Trop de tentatives de connexion. Réessayez plus tard."));
    }

    private String register(String email) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setNom("Durand");
        request.setPrenom("Alice");
        request.setEmail(email);
        request.setMotDePasse("password123");

        return mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String firstServiceId() throws Exception {
        String responseBody = mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode services = objectMapper.readTree(responseBody);
        return services.get(0).get("serviceId").asText();
    }

    private String buyTicket(String authToken, String clientId, String serviceId) throws Exception {
        AchatBilletRequest request = new AchatBilletRequest();
        request.setServiceId(serviceId);
        request.setClientId(clientId);
        request.setProfilTarifaire("STANDARD");
        request.setClasseReservation("SECONDE");
        request.setMethodePaiement("CARTE");

        String responseBody = mockMvc.perform(post("/api/achat/confirmer")
                        .header("X-Auth-Token", authToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).get("billetId").asText();
    }
}
