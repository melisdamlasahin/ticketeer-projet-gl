package ticket_train.ticketeer.controller;

import ticket_train.ticketeer.dto.ValidationRequest;
import ticket_train.ticketeer.dto.ValidationResponse;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.service.ControlUnitAuthService;
import ticket_train.ticketeer.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/controleur/api")
public class ValidationApiController {

    private final ValidationService validationService;
    private final ControlUnitAuthService controlUnitAuthService;

    public ValidationApiController(ValidationService validationService,
                                   ControlUnitAuthService controlUnitAuthService) {
        this.validationService = validationService;
        this.controlUnitAuthService = controlUnitAuthService;
    }

    @PostMapping("/valider")
    public ResponseEntity<ValidationResponse> validerBillet(@RequestBody ValidationRequest request,
                                                            Authentication authentication) {
        String login = authentication.getName();
        Controleur controleur = controlUnitAuthService.findByLogin(login)
                .orElse(null);

        if (controleur == null) {
            return ResponseEntity.status(403).build();
        }

        ValidationResponse response = validationService.validerBillet(request, controleur);
        return ResponseEntity.ok(response);
    }
}
