package ticket_train.ticketeer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ticket_train.ticketeer.dto.mobile.AchatBilletRequest;
import ticket_train.ticketeer.dto.mobile.LoginRequest;
import ticket_train.ticketeer.dto.mobile.RegisterRequest;
import ticket_train.ticketeer.service.MobileApiService;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
class MobileApiValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MobileApiService mobileApiService;
    @MockitoBean
    private ticket_train.ticketeer.service.ControlUnitAuthService controlUnitAuthService;
    @MockitoBean
    private ticket_train.ticketeer.service.ValidationService validationService;

    @Test
    void loginRejectsInvalidEmail() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("not-an-email");
        request.setMotDePasse("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation de la requete echouee"))
                .andExpect(jsonPath("$.fieldErrors.email").value("L'email doit etre valide"));

        verifyNoInteractions(mobileApiService);
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setNom("Dupont");
        request.setPrenom("Jean");
        request.setEmail("jean@example.com");
        request.setMotDePasse("short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.motDePasse")
                        .value("Le mot de passe doit contenir au moins 8 caracteres"));

        verifyNoInteractions(mobileApiService);
    }

    @Test
    void achatTarifRejectsMissingRequiredFields() throws Exception {
        AchatBilletRequest request = new AchatBilletRequest();
        request.setProfilTarifaire("STANDARD");

        mockMvc.perform(post("/api/achat/tarif")
                        .with(user(UUID.randomUUID().toString()).roles("MOBILE_CLIENT"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.serviceId").value("Le service aller est obligatoire"))
                .andExpect(jsonPath("$.fieldErrors.clientId").value("Le client est obligatoire"));

        verifyNoInteractions(mobileApiService);
    }
}
