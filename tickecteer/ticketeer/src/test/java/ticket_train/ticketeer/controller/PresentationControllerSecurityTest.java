package ticket_train.ticketeer.controller;

import org.junit.jupiter.api.DisplayName;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Presentation demo: Spring Security on controller routes")
class PresentationControllerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ControlUnitAuthService controlUnitAuthService;
    @MockitoBean private ValidationService validationService;
    @MockitoBean private ValidationTraceService validationTraceService;

    @Test
    @DisplayName("Anonymous users are redirected to the controller login page")
    void anonymousUserCannotAccessControllerArea() throws Exception {
        mockMvc.perform(get("/controleur/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/controleur/login"));
    }

    @Test
    @WithMockUser(username = "mobile-client", roles = "MOBILE_CLIENT")
    @DisplayName("A logged-in mobile client cannot access controller validation endpoints")
    void nonControllerRoleCannotValidateTickets() throws Exception {
        mockMvc.perform(post("/controleur/api/valider")
                        .contentType("application/json")
                        .content("""
                                {
                                  "codeOptique": "CODE-DEMO",
                                  "serviceId": "00000000-0000-0000-0000-000000000001",
                                  "checkpointId": "00000000-0000-0000-0000-000000000002"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "demo-controller", roles = "CONTROLEUR")
    @DisplayName("A CONTROLEUR can call validation and receives the business result")
    void controllerRoleCanValidateTickets() throws Exception {
        when(controlUnitAuthService.findByLogin("demo-controller"))
                .thenReturn(Optional.of(new Controleur("demo-controller", "bcrypt-hash", "Nom", "Prenom")));
        when(validationService.validerBillet(any(), any()))
                .thenReturn(new ticket_train.ticketeer.dto.ValidationResponse(
                        ValidationResult.VALID,
                        ValidationMotif.OK
                ));

        mockMvc.perform(post("/controleur/api/valider")
                        .contentType("application/json")
                        .content("""
                                {
                                  "codeOptique": "CODE-DEMO",
                                  "serviceId": "00000000-0000-0000-0000-000000000001",
                                  "checkpointId": "00000000-0000-0000-0000-000000000002"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultat", is("VALID")))
                .andExpect(jsonPath("$.motif", is("OK")));
    }
}
