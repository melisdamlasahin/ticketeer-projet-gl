package ticket_train.ticketeer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/controleur/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Identifiant ou mot de passe incorrect.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Vous avez ete deconnecte avec succes.");
        }
        return "controleur/login";
    }
}
