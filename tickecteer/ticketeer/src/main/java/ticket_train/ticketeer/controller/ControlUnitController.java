package ticket_train.ticketeer.controller;

import ticket_train.ticketeer.dto.ScanBilletForm;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.repository.ServiceFerroviaireRepository;
import ticket_train.ticketeer.service.ControlUnitAuthService;
import ticket_train.ticketeer.service.ValidationTraceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/controleur")
public class ControlUnitController {

    private final ServiceFerroviaireRepository serviceFerroviaireRepository;
    private final ControlUnitAuthService controlUnitAuthService;
    private final ValidationTraceService validationTraceService;

    public ControlUnitController(ServiceFerroviaireRepository serviceFerroviaireRepository,
                                 ControlUnitAuthService controlUnitAuthService,
                                 ValidationTraceService validationTraceService) {
        this.serviceFerroviaireRepository = serviceFerroviaireRepository;
        this.controlUnitAuthService = controlUnitAuthService;
        this.validationTraceService = validationTraceService;
    }

    @Transactional(readOnly = true)
    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        String login = authentication.getName();
        Controleur controleur = controlUnitAuthService.findByLogin(login).orElse(null);
        if (controleur != null) {
            model.addAttribute("controleurNom", controleur.getPrenom() + " " + controleur.getNom());
            model.addAttribute("recentValidations", validationTraceService.getRecentTracesForController(controleur));
        }

        List<ServiceFerroviaire> services = serviceFerroviaireRepository.findAll();
        model.addAttribute("services", services);
        return "controleur/home";
    }

    @Transactional(readOnly = true)
    @GetMapping("/scan")
    public String scanPage(@RequestParam("serviceId") UUID serviceId,
                           Authentication authentication,
                           Model model) {
        String login = authentication.getName();
        Controleur controleur = controlUnitAuthService.findByLogin(login).orElse(null);
        if (controleur != null) {
            model.addAttribute("controleurNom", controleur.getPrenom() + " " + controleur.getNom());
        }

        ServiceFerroviaire service = serviceFerroviaireRepository.findById(serviceId).orElse(null);
        if (service == null) {
            return "redirect:/controleur/home";
        }

        model.addAttribute("service", service);
        model.addAttribute("serviceLabel",
                service.getTrain().getNomTrain() + " - " +
                service.getVilleDepart().getNom() + " vers " +
                service.getVilleArrivee().getNom() + " (" +
                service.getDateTrajet() +
                (service.getHeureDepart() != null ? " " + service.getHeureDepart() : "") +
                ")");
        model.addAttribute("checkpoints", service.getCheckpoints().stream()
                .sorted(Comparator.comparing(cp -> cp.getOrdre()))
                .toList());
        model.addAttribute("scanForm", new ScanBilletForm());
        return "controleur/scan";
    }
}
