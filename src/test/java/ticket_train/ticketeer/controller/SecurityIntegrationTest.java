package ticket_train.ticketeer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ticket_train.ticketeer.service.ControlUnitAuthService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ControlUnitAuthService controlUnitAuthService;
    @MockitoBean
    private ticket_train.ticketeer.service.ValidationService validationService;

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/controleur/login"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedHomeRedirectsWhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/controleur/api/valider")
                        .contentType("application/json")
                        .content("{\"codeOptique\":\"CODE\",\"serviceId\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/controleur/login"));
    }
}
