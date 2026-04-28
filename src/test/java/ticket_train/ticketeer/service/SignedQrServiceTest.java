package ticket_train.ticketeer.service;

import org.junit.jupiter.api.Test;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceFerroviaire;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignedQrServiceTest {

    private final SignedQrService signedQrService = new SignedQrService("test-secret");

    @Test
    void buildSignedPayloadUsesTicketeerPrefix() {
        String payload = signedQrService.buildSignedPayload(sampleBillet());

        assertTrue(payload.startsWith("TICKETEERQR."));
    }

    @Test
    void parseAndVerifyAcceptsLegacyEasyRailPrefix() {
        String payload = signedQrService.buildSignedPayload(sampleBillet());
        String legacyPayload = payload.replaceFirst("^TICKETEERQR\\.", "EASYRAILQR.");

        SignedQrService.ParseResult result = signedQrService.parseAndVerify(legacyPayload);

        assertEquals(SignedQrService.ParseStatus.VALID, result.getStatus());
        assertTrue(result.getPayload().isPresent());
    }

    private Billet sampleBillet() {
        Client client = new Client();
        client.setClientId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        ServiceFerroviaire service = new ServiceFerroviaire();
        service.setServiceId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

        SegmentBillet segment = new SegmentBillet();
        segment.setService(service);

        Billet billet = new Billet();
        billet.setTicketId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        billet.setCodeOptique("TICKET-001");
        billet.setClient(client);
        billet.setDateEmission(LocalDateTime.of(2026, 4, 27, 11, 0));
        billet.setPrixFinal(new BigDecimal("49.90"));
        billet.setSegments(List.of(segment));
        return billet;
    }
}
