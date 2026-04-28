package ticket_train.ticketeer.controller;

import ticket_train.ticketeer.dto.ValidationRequest;
import ticket_train.ticketeer.dto.ValidationResponse;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.service.ControllerValidationRateLimitService;
import ticket_train.ticketeer.service.ControlUnitAuthService;
import ticket_train.ticketeer.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/controleur/api")
public class ValidationApiController {

    private final ValidationService validationService;
    private final ControlUnitAuthService controlUnitAuthService;
    private final ControllerValidationRateLimitService controllerValidationRateLimitService;

    public ValidationApiController(ValidationService validationService,
                                   ControlUnitAuthService controlUnitAuthService,
                                   ControllerValidationRateLimitService controllerValidationRateLimitService) {
        this.validationService = validationService;
        this.controlUnitAuthService = controlUnitAuthService;
        this.controllerValidationRateLimitService = controllerValidationRateLimitService;
    }

    @PostMapping("/valider")
    public ResponseEntity<ValidationResponse> validerBillet(@RequestBody ValidationRequest request,
                                                            Authentication authentication,
                                                            HttpServletRequest httpRequest) {
        String login = authentication.getName();
        Controleur controleur = controlUnitAuthService.findByLogin(login)
                .orElse(null);

        if (controleur == null) {
            return ResponseEntity.status(403).build();
        }

        controllerValidationRateLimitService.checkAllowed(login + "|" + resolveSourceIp(httpRequest));
        ValidationResponse response = validationService.validerBillet(request, controleur);
        return ResponseEntity.ok(response);
    }

    private String resolveSourceIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int commaIndex = forwarded.indexOf(',');
            return commaIndex >= 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
