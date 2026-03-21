package ticket_train.ticketeer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.service.ControlUnitAuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/control-unit")
public class ControlUnitController {
    private final ControlUnitAuthService controlUnitAuthService;

    public ControlUnitController(ControlUnitAuthService controlUnitAuthService) {
        this.controlUnitAuthService = controlUnitAuthService;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me() {
        Controleur controleur = controlUnitAuthService.getAuthenticatedControleur();
        return ResponseEntity.ok(Map.of(
                "login", controleur.getLogin(),
                "nom", controleur.getNom(),
                "prenom", controleur.getPrenom()
        ));
    }
}
