package ticket_train.ticketeer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.service.ControlUnitAuthService;
import ticket_train.ticketeer.service.ValidationTraceService;
import ticket_train.ticketeer.service.ValidationService;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ControlUnitAuthService controlUnitAuthService;
    @MockitoBean
    private ValidationService validationService;
    @MockitoBean
    private ValidationTraceService validationTraceService;

    @Test
    void validationApiRedirectsAnonymousController() throws Exception {
        mockMvc.perform(post("/controleur/api/valider")
                        .contentType("application/json")
                        .content("{\"codeOptique\":\"CODE\",\"serviceId\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/controleur/login"));
    }

    @Test
    @WithMockUser(username = "client", roles = "MOBILE_CLIENT")
    void validationApiRejectsAuthenticatedNonController() throws Exception {
        mockMvc.perform(post("/controleur/api/valider")
                        .contentType("application/json")
                        .content("{\"codeOptique\":\"CODE\",\"serviceId\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ghost", roles = "CONTROLEUR")
    void validationApiRejectsUnknownControllerRecord() throws Exception {
        when(controlUnitAuthService.findByLogin("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(post("/controleur/api/valider")
                        .contentType("application/json")
                        .content("{\"codeOptique\":\"CODE\",\"serviceId\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void controllerPagesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/controleur/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/controleur/login"));

        mockMvc.perform(get("/controleur/scan").param("serviceId", UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/controleur/login"));
    }

    @Test
    @WithMockUser(username = "nathan", roles = "CONTROLEUR")
    void controllerHomeAllowsKnownController() throws Exception {
        when(controlUnitAuthService.findByLogin("nathan"))
                .thenReturn(Optional.of(new Controleur("nathan", "hash", "Nom", "Prenom")));

        mockMvc.perform(get("/controleur/home"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Selection du service ferroviaire")));
    }

    @Test
    void loginPageRendersThymeleafTemplate() throws Exception {
        mockMvc.perform(get("/controleur/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ticketeer")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"login\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"password\"")));
    }

    @Test
    @WithMockUser(username = "nathan", roles = "CONTROLEUR")
    void validationApiReturnsBusinessFailureInsteadOfHttp500WhenServiceFails() throws Exception {
        when(controlUnitAuthService.findByLogin("nathan"))
                .thenReturn(Optional.of(new Controleur("nathan", "hash", "Nom", "Prenom")));
        when(validationService.validerBillet(any(), any()))
                .thenThrow(new IllegalStateException("simulated persistence failure"));

        mockMvc.perform(post("/controleur/api/valider")
                        .contentType("application/json")
                        .content("""
                                {
                                  "codeOptique": "SNCF-20260514-PL-9M2X74",
                                  "serviceId": "00000000-0000-0000-0000-000000000001",
                                  "checkpointId": "00000000-0000-0000-0000-000000000003"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultat", is(ValidationResult.INVALID.name())))
                .andExpect(jsonPath("$.motif", is(ValidationMotif.VALIDATION_IMPOSSIBLE_TEMPORAIREMENT.name())));
    }
}
