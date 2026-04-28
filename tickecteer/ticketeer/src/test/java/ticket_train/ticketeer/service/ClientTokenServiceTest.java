package ticket_train.ticketeer.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientTokenServiceTest {

    @Test
    void tokenRemainsValidAcrossServiceInstancesWithSameSecret() {
        Clock issueClock = Clock.fixed(Instant.parse("2026-05-09T10:00:00Z"), ZoneOffset.UTC);
        ClientTokenService issuer = new ClientTokenService("shared-secret", Duration.ofHours(12), issueClock);
        UUID clientId = UUID.randomUUID();

        String token = issuer.issueToken(clientId);

        Clock restartedClock = Clock.fixed(Instant.parse("2026-05-09T11:00:00Z"), ZoneOffset.UTC);
        ClientTokenService afterRestart = new ClientTokenService("shared-secret", Duration.ofHours(12), restartedClock);
        Optional<UUID> resolved = afterRestart.resolveClientId(token);

        assertTrue(resolved.isPresent());
        assertEquals(clientId, resolved.get());
    }

    @Test
    void expiredTokenIsRejected() {
        Clock issueClock = Clock.fixed(Instant.parse("2026-05-09T10:00:00Z"), ZoneOffset.UTC);
        ClientTokenService issuer = new ClientTokenService("shared-secret", Duration.ofHours(1), issueClock);
        String token = issuer.issueToken(UUID.randomUUID());

        Clock expiredClock = Clock.fixed(Instant.parse("2026-05-09T12:30:00Z"), ZoneOffset.UTC);
        ClientTokenService afterExpiry = new ClientTokenService("shared-secret", Duration.ofHours(1), expiredClock);

        assertTrue(afterExpiry.resolveClientId(token).isEmpty());
    }
}
