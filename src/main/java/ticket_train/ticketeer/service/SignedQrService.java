package ticket_train.ticketeer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.SegmentBillet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SignedQrService {

    public enum ParseStatus {
        NOT_SIGNED,
        VALID,
        INVALID_SIGNATURE,
        MALFORMED
    }

    private static final String PREFIX = "EASYRAILQR";
    private static final Pattern JSON_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\":\"?([^\",}]*)\"?");

    private final String qrSigningSecret;

    public SignedQrService(@Value("${app.qr-signing-secret}") String qrSigningSecret) {
        this.qrSigningSecret = qrSigningSecret;
    }

    public String buildSignedPayload(Billet billet) {
        SegmentBillet segment = billet.getSegments().isEmpty() ? null : billet.getSegments().get(0);
        String serviceId = segment != null ? segment.getService().getServiceId().toString() : "";
        String payloadJson = "{"
                + "\"v\":1,"
                + "\"billetId\":\"" + billet.getTicketId() + "\","
                + "\"codeOptique\":\"" + billet.getCodeOptique() + "\","
                + "\"clientId\":\"" + billet.getClient().getClientId() + "\","
                + "\"serviceId\":\"" + serviceId + "\","
                + "\"dateEmission\":\"" + billet.getDateEmission() + "\","
                + "\"prixFinal\":\"" + billet.getPrixFinal() + "\""
                + "}";
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return PREFIX + "." + encodedPayload + "." + signature;
    }

    public ParseResult parseAndVerify(String rawValue) {
        if (rawValue == null || rawValue.isBlank() || !rawValue.startsWith(PREFIX + ".")) {
            return new ParseResult(ParseStatus.NOT_SIGNED, null);
        }

        String[] parts = rawValue.split("\\.", 3);
        if (parts.length != 3) {
            return new ParseResult(ParseStatus.MALFORMED, null);
        }

        String encodedPayload = parts[1];
        String providedSignature = parts[2];
        String expectedSignature = sign(encodedPayload);
        if (!constantTimeEquals(providedSignature, expectedSignature)) {
            return new ParseResult(ParseStatus.INVALID_SIGNATURE, null);
        }

        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            return new ParseResult(ParseStatus.VALID, parsePayload(payloadJson));
        } catch (Exception ex) {
            return new ParseResult(ParseStatus.MALFORMED, null);
        }
    }

    private SignedQrPayload parsePayload(String json) {
        String billetId = extract(json, "billetId");
        String codeOptique = extract(json, "codeOptique");
        String clientId = extract(json, "clientId");
        String serviceId = extract(json, "serviceId");
        if (billetId == null || codeOptique == null || clientId == null) {
            throw new IllegalArgumentException("Missing QR fields");
        }

        return new SignedQrPayload(
                UUID.fromString(billetId),
                codeOptique,
                UUID.fromString(clientId),
                serviceId == null || serviceId.isBlank() ? null : UUID.fromString(serviceId)
        );
    }

    private String extract(String json, String field) {
        Matcher matcher = JSON_FIELD_PATTERN.matcher(json);
        while (matcher.find()) {
            if (field.equals(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        return null;
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    qrSigningSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Unable to sign QR payload", ex);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < leftBytes.length; i++) {
            diff |= leftBytes[i] ^ rightBytes[i];
        }
        return diff == 0;
    }

    public static final class SignedQrPayload {
        private final UUID billetId;
        private final String codeOptique;
        private final UUID clientId;
        private final UUID serviceId;

        public SignedQrPayload(UUID billetId, String codeOptique, UUID clientId, UUID serviceId) {
            this.billetId = billetId;
            this.codeOptique = codeOptique;
            this.clientId = clientId;
            this.serviceId = serviceId;
        }

        public UUID getBilletId() {
            return billetId;
        }

        public String getCodeOptique() {
            return codeOptique;
        }

        public UUID getClientId() {
            return clientId;
        }

        public UUID getServiceId() {
            return serviceId;
        }
    }

    public static final class ParseResult {
        private final ParseStatus status;
        private final SignedQrPayload payload;

        public ParseResult(ParseStatus status, SignedQrPayload payload) {
            this.status = status;
            this.payload = payload;
        }

        public ParseStatus getStatus() {
            return status;
        }

        public Optional<SignedQrPayload> getPayload() {
            return Optional.ofNullable(payload);
        }
    }
}
