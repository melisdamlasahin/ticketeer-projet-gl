package ticket_train.ticketeer.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    public AuthResponse login(@RequestBody LoginRequest request) {
        return mobileApiService.login(request);
    }

    @PostMapping("/auth/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return mobileApiService.register(request);
    }

    @GetMapping("/clients/{clientId}/profile")
    public ClientProfileResponse getProfile(@RequestHeader("X-Auth-Token") String authToken,
                                            @PathVariable String clientId) {
        return mobileApiService.getProfile(authToken, clientId);
    }

    @PutMapping("/clients/{clientId}/profile")
    public ClientProfileResponse updateProfile(@RequestHeader("X-Auth-Token") String authToken,
                                               @PathVariable String clientId,
                                               @RequestBody UpdateProfileRequest request) {
        return mobileApiService.updateProfile(authToken, clientId, request);
    }

    @PostMapping("/achat/tarif")
    public TarificationResponse calculerTarif(@RequestHeader("X-Auth-Token") String authToken,
                                              @RequestBody AchatBilletRequest request) {
        return mobileApiService.calculerTarif(authToken, request);
    }

    @PostMapping("/achat/confirmer")
    public AchatBilletResponse confirmerAchat(@RequestHeader("X-Auth-Token") String authToken,
                                              @RequestBody AchatBilletRequest request) {
        return mobileApiService.confirmerAchat(authToken, request);
    }

    @GetMapping("/billets/{id}")
    public TicketResponse getBillet(@RequestHeader("X-Auth-Token") String authToken,
                                    @PathVariable("id") String billetId) {
        return mobileApiService.getBillet(authToken, billetId);
    }

    @GetMapping("/billets/client/{clientId}")
    public List<TicketResponse> getBilletsByClient(@RequestHeader("X-Auth-Token") String authToken,
                                                   @PathVariable String clientId) {
        return mobileApiService.getBilletsByClient(authToken, clientId);
    }

    @GetMapping("/billets/{id}/pdf")
    public ResponseEntity<byte[]> getBilletPdf(@RequestHeader("X-Auth-Token") String authToken,
                                               @PathVariable("id") String billetId) {
        byte[] pdf = mobileApiService.buildTicketPdf(authToken, billetId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ticket-" + billetId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/billets/{id}/cancel")
    public TicketResponse cancelBillet(@RequestHeader("X-Auth-Token") String authToken,
                                       @PathVariable("id") String billetId) {
        return mobileApiService.cancelBillet(authToken, billetId);
    }

    @PutMapping("/billets/{id}")
    public TicketResponse updateBillet(@RequestHeader("X-Auth-Token") String authToken,
                                       @PathVariable("id") String billetId,
                                       @RequestBody AchatBilletRequest request) {
        return mobileApiService.updateBillet(authToken, billetId, request);
    }
}
