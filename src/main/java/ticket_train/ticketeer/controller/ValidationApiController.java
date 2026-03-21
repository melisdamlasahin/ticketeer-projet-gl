package ticket_train.ticketeer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ticket_train.ticketeer.dto.ValidationRequest;
import ticket_train.ticketeer.dto.ValidationResponse;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.service.ControlUnitAuthService;
import ticket_train.ticketeer.service.ValidationService;

@RestController
@RequestMapping("/api/validations")
public class ValidationApiController {
    private final ValidationService validationService;
    private final ControlUnitAuthService controlUnitAuthService;

    public ValidationApiController(
            ValidationService validationService,
            ControlUnitAuthService controlUnitAuthService
    ) {
        this.validationService = validationService;
        this.controlUnitAuthService = controlUnitAuthService;
    }

    @PostMapping
    public ResponseEntity<ValidationResponse> validate(@RequestBody ValidationRequest request) {
        Controleur controleur = controlUnitAuthService.getAuthenticatedControleur();
        ValidationResponse response = validationService.validateTicket(
                request.getCodeOptique(),
                request.getServiceId(),
                request.getTimestamp(),
                controleur
        );
        return ResponseEntity.ok(response);
    }
}
