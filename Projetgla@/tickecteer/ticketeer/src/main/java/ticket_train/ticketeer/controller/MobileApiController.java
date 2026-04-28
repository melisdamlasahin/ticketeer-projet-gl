package ticket_train.ticketeer.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import ticket_train.ticketeer.dto.mobile.AchatBilletRequest;
import ticket_train.ticketeer.dto.mobile.AchatBilletResponse;
import ticket_train.ticketeer.dto.mobile.AuthResponse;
import ticket_train.ticketeer.dto.mobile.ClientProfileResponse;
import ticket_train.ticketeer.dto.mobile.LoginRequest;
import ticket_train.ticketeer.dto.mobile.RegisterRequest;
import ticket_train.ticketeer.dto.mobile.ServiceResponse;
import ticket_train.ticketeer.dto.mobile.TarificationResponse;
import ticket_train.ticketeer.dto.mobile.TicketResponse;
import ticket_train.ticketeer.dto.mobile.UpdateProfileRequest;
import ticket_train.ticketeer.service.MobileApiService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MobileApiController {

    private final MobileApiService mobileApiService;

    public MobileApiController(MobileApiService mobileApiService) {
        this.mobileApiService = mobileApiService;
    }

    @GetMapping("/services")
    public List<ServiceResponse> getServices() {
        return mobileApiService.getServices();
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return mobileApiService.login(request, extractSourceIp(httpRequest));
    }

    @PostMapping("/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return mobileApiService.register(request, extractSourceIp(httpRequest));
    }

    @PostMapping("/auth/logout")
    public AuthResponse logout(HttpServletRequest httpRequest) {
        return mobileApiService.logout(extractSourceIp(httpRequest));
    }

    @GetMapping("/clients/{clientId}/profile")
    public ClientProfileResponse getProfile(@PathVariable String clientId) {
        return mobileApiService.getProfile(clientId);
    }

    @PutMapping("/clients/{clientId}/profile")
    public ClientProfileResponse updateProfile(@PathVariable String clientId,
                                               @Valid @RequestBody UpdateProfileRequest request) {
        return mobileApiService.updateProfile(clientId, request);
    }

    @PostMapping("/achat/tarif")
    public TarificationResponse calculerTarif(@Valid @RequestBody AchatBilletRequest request) {
        return mobileApiService.calculerTarif(request);
    }

    @PostMapping("/achat/confirmer")
    public AchatBilletResponse confirmerAchat(@Valid @RequestBody AchatBilletRequest request) {
        return mobileApiService.confirmerAchat(request);
    }

    @GetMapping("/billets/{id}")
    public TicketResponse getBillet(@PathVariable("id") String billetId) {
        return mobileApiService.getBillet(billetId);
    }

    @GetMapping("/billets/client/{clientId}")
    public List<TicketResponse> getBilletsByClient(@PathVariable String clientId) {
        return mobileApiService.getBilletsByClient(clientId);
    }

    @GetMapping("/billets/{id}/pdf")
    public ResponseEntity<byte[]> getBilletPdf(@PathVariable("id") String billetId) {
        byte[] pdf = mobileApiService.buildTicketPdf(billetId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ticket-" + billetId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/billets/{id}/cancel")
    public TicketResponse cancelBillet(@PathVariable("id") String billetId) {
        return mobileApiService.cancelBillet(billetId);
    }

    @PutMapping("/billets/{id}")
    public TicketResponse updateBillet(@PathVariable("id") String billetId,
                                       @Valid @RequestBody AchatBilletRequest request) {
        return mobileApiService.updateBillet(billetId, request);
    }

    private String extractSourceIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int commaIndex = forwarded.indexOf(',');
            return commaIndex >= 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
