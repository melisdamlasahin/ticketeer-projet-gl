package ticket_train.ticketeer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClientTokenService {

    private static final String TOKEN_PREFIX = "mt1";

    private final SecretKeySpec signingKey;
    private final Duration tokenTtl;
    private final Clock clock;
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    @Autowired
    public ClientTokenService(@Value("${app.mobile-auth-token-secret:${app.qr-signing-secret}}") String secret,
                              @Value("${app.mobile-auth-token-ttl-hours:168}") long tokenTtlHours) {
        this(secret, Duration.ofHours(tokenTtlHours), Clock.systemUTC());
    }

    ClientTokenService(String secret, Duration tokenTtl, Clock clock) {
        this.signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.tokenTtl = tokenTtl;
        this.clock = clock;
    }

    public String issueToken(UUID clientId) {
        long issuedAtEpoch = Instant.now(clock).getEpochSecond();
        long expiresAtEpoch = Instant.now(clock).plus(tokenTtl).getEpochSecond();
        String payload = clientId + "." + issuedAtEpoch + "." + expiresAtEpoch;
        String encodedPayload = base64UrlEncode(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return TOKEN_PREFIX + "." + encodedPayload + "." + signature;
    }

    public Optional<UUID> resolveClientId(String token) {
        String normalizedToken = normalizeToken(token);
        if (normalizedToken == null || isRevoked(normalizedToken)) {
            return Optional.empty();
        }
        return parseToken(normalizedToken).map(TokenClaims::clientId);
    }

    public UUID requireAuthenticatedClientId() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Auth token invalide");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID uuid) {
            return uuid;
        }
        if (principal instanceof String value) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Auth token invalide");
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Auth token invalide");
    }

    public Optional<TokenClaims> parseToken(String token) {
        String normalizedToken = normalizeToken(token);
        if (normalizedToken == null) {
            return Optional.empty();
        }

        String[] parts = normalizedToken.split("\\.");
        if (parts.length != 3 || !TOKEN_PREFIX.equals(parts[0])) {
            return Optional.empty();
        }
        if (!constantTimeEquals(parts[2], sign(parts[1]))) {
            return Optional.empty();
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String[] payloadParts = payload.split("\\.");
            if (payloadParts.length != 3) {
                return Optional.empty();
            }

            UUID clientId = UUID.fromString(payloadParts[0]);
            Instant issuedAt = Instant.ofEpochSecond(Long.parseLong(payloadParts[1]));
            Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(payloadParts[2]));
            if (!expiresAt.isAfter(Instant.now(clock))) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(clientId, issuedAt, expiresAt));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public boolean revokeToken(String token) {
        String normalizedToken = normalizeToken(token);
        if (normalizedToken == null) {
            return false;
        }
        Optional<TokenClaims> claims = parseToken(normalizedToken);
        if (claims.isEmpty()) {
            return false;
        }
        revokedTokens.put(normalizedToken, claims.get().expiresAt());
        return true;
    }

    public boolean isRevoked(String token) {
        String normalizedToken = normalizeToken(token);
        if (normalizedToken == null) {
            return false;
        }
        Instant revokedUntil = revokedTokens.get(normalizedToken);
        if (revokedUntil == null) {
            return false;
        }
        Instant now = Instant.now(clock);
        if (!revokedUntil.isAfter(now)) {
            revokedTokens.remove(normalizedToken);
            return false;
        }
        return true;
    }

    public String requireAuthenticatedToken() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Auth token invalide");
        }
        Object credentials = authentication.getCredentials();
        if (credentials instanceof String token && !token.isBlank()) {
            return token;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Auth token invalide");
    }

    private String normalizeToken(String token) {
        if (token == null) {
            return null;
        }
        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String bearerValue = trimmed.substring(7).trim();
            return bearerValue.isEmpty() ? null : bearerValue;
        }
        return trimmed;
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return base64UrlEncode(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to sign mobile auth token", ex);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    public record TokenClaims(UUID clientId, Instant issuedAt, Instant expiresAt) {
    }
}
