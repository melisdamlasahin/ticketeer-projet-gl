package ticket_train.ticketeer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ticket_train.ticketeer.dto.ValidationRequest;
import ticket_train.ticketeer.dto.ValidationResponse;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import ticket_train.ticketeer.service.ControlUnitAuthService;
import ticket_train.ticketeer.service.ValidationService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ValidationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ValidationService validationService;
    @MockitoBean
    private ControlUnitAuthService controlUnitAuthService;

    @Test
    @WithMockUser(username = "nathan", roles = "CONTROLEUR")
    void authenticatedControllerCanValidateTicket() throws Exception {
        ValidationResponse response = new ValidationResponse(ValidationResult.INVALID, ValidationMotif.DEJA_VALIDE);
        when(controlUnitAuthService.findByLogin("nathan")).thenReturn(Optional.of(new Controleur("nathan", "hash", "Petit", "Paul")));
        when(validationService.validerBillet(any(), any())).thenReturn(response);

        ValidationRequest request = new ValidationRequest("CODE", UUID.randomUUID());

        mockMvc.perform(post("/controleur/api/valider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motif").value("DEJA_VALIDE"))
                .andExpect(jsonPath("$.resultat").value("INVALID"));
    }

    @Test
    @WithMockUser(username = "ghost", roles = "CONTROLEUR")
    void authenticatedUnknownControllerGetsForbidden() throws Exception {
        when(controlUnitAuthService.findByLogin("ghost")).thenReturn(Optional.empty());

        ValidationRequest request = new ValidationRequest("CODE", UUID.randomUUID());

        mockMvc.perform(post("/controleur/api/valider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
