package ticket_train.ticketeer.service;

import org.junit.jupiter.api.Test;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.model.Train;
import ticket_train.ticketeer.model.Ville;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignedQrTamperingIntegrationTest {

    private final SignedQrService signedQrService = new SignedQrService("test-secret");

    @Test
    void validSignedQrParsesTicketPayload() {
        Billet billet = buildBillet();

        SignedQrService.ParseResult result = signedQrService.parseAndVerify(signedQrService.buildSignedPayload(billet));

        assertEquals(SignedQrService.ParseStatus.VALID, result.getStatus());
        assertTrue(result.getPayload().isPresent());
        assertEquals(billet.getTicketId(), result.getPayload().orElseThrow().getBilletId());
        assertEquals(billet.getCodeOptique(), result.getPayload().orElseThrow().getCodeOptique());
        assertEquals(billet.getClient().getClientId(), result.getPayload().orElseThrow().getClientId());
    }

    @Test
    void modifiedBilletIdInvalidatesSignature() {
        Billet billet = buildBillet();
        String tampered = replacePayloadField(signedQrService.buildSignedPayload(billet), "billetId", UUID.randomUUID().toString());

        assertEquals(SignedQrService.ParseStatus.INVALID_SIGNATURE, signedQrService.parseAndVerify(tampered).getStatus());
    }

    @Test
    void modifiedClientIdInvalidatesSignature() {
        Billet billet = buildBillet();
        String tampered = replacePayloadField(signedQrService.buildSignedPayload(billet), "clientId", UUID.randomUUID().toString());

        assertEquals(SignedQrService.ParseStatus.INVALID_SIGNATURE, signedQrService.parseAndVerify(tampered).getStatus());
    }

    @Test
    void malformedSignedPayloadIsRejected() {
        assertEquals(SignedQrService.ParseStatus.MALFORMED, signedQrService.parseAndVerify("TICKETEERQR.missing-signature").getStatus());
    }

    private Billet buildBillet() {
        Client client = new Client("Dupont", "Jean", "signed-" + UUID.randomUUID() + "@test", "hash", "photo");
        client.setClientId(UUID.randomUUID());
        ServiceFerroviaire service = new ServiceFerroviaire(
                LocalDate.of(2026, 5, 10),
                LocalTime.of(10, 0),
                new Train("T-" + UUID.randomUUID(), "TGV Test"),
                new Ville("Paris"),
                new Ville("Lyon"),
                50.0
        );
        service.setServiceId(UUID.randomUUID());
        Billet billet = new Billet("CODE-" + UUID.randomUUID(), BigDecimal.TEN, client);
        billet.setTicketId(UUID.randomUUID());
        SegmentBillet segment = new SegmentBillet(1, service);
        segment.setBillet(billet);
        billet.setSegments(List.of(segment));
        return billet;
    }

    private String replacePayloadField(String signedPayload, String field, String replacement) {
        String[] parts = signedPayload.split("\\.", 3);
        String json = new String(Base64.getUrlDecoder().decode(parts[1]));
        json = json.replaceFirst("\"" + field + "\":\"[^\"]+\"", "\"" + field + "\":\"" + replacement + "\"");
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
        return parts[0] + "." + encoded + "." + parts[2];
    }
}
