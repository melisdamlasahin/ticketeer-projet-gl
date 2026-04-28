package ticket_train.ticketeer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceCheckpoint;
import ticket_train.ticketeer.model.enums.ValidationMotif;

import java.util.UUID;

@Service
public class SecurityAuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void logLoginSuccess(String email, UUID clientId, String sourceIp) {
        LOGGER.info("event=login_success email={} clientId={} ip={}", email, clientId, sourceIp);
    }

    public void logLoginFailure(String email, String sourceIp, String reason) {
        LOGGER.warn("event=login_failure email={} ip={} reason={}", email, sourceIp, reason);
    }

    public void logLoginRateLimited(String email, String sourceIp) {
        LOGGER.warn("event=login_rate_limited email={} ip={}", email, sourceIp);
    }

    public void logLogout(UUID clientId, String sourceIp) {
        LOGGER.info("event=logout clientId={} ip={}", clientId, sourceIp);
    }

    public void logRegistration(String email, UUID clientId, String sourceIp) {
        LOGGER.info("event=register email={} clientId={} ip={}", email, clientId, sourceIp);
    }

    public void logMissingToken(String path, String sourceIp) {
        LOGGER.warn("event=missing_token path={} ip={}", path, sourceIp);
    }

    public void logInvalidToken(String path, String sourceIp) {
        LOGGER.warn("event=invalid_token path={} ip={}", path, sourceIp);
    }

    public void logRevokedTokenUse(String path, String sourceIp) {
        LOGGER.warn("event=revoked_token path={} ip={}", path, sourceIp);
    }

    public void logForbiddenClientAccess(UUID authenticatedClientId, String requestedClientId) {
        LOGGER.warn("event=forbidden_client_access authClientId={} requestedClientId={}", authenticatedClientId, requestedClientId);
    }

    public void logForbiddenTicketAccess(UUID authenticatedClientId, UUID billetId) {
        LOGGER.warn("event=forbidden_ticket_access authClientId={} billetId={}", authenticatedClientId, billetId);
    }

    public void logFraudDetection(ValidationMotif motif,
                                  Controleur controleur,
                                  SegmentBillet segment,
                                  ServiceCheckpoint checkpoint) {
        LOGGER.warn(
                "event=fraud_detection motif={} controleur={} segmentId={} serviceId={} checkpointOrder={}",
                motif,
                controleur != null ? controleur.getLogin() : "unknown",
                segment != null ? segment.getSegmentId() : null,
                segment != null && segment.getService() != null ? segment.getService().getServiceId() : null,
                checkpoint != null ? checkpoint.getOrdre() : null
        );
    }

    public void logJourneyWindowViolation(ValidationMotif motif,
                                          SegmentBillet segment,
                                          ServiceCheckpoint checkpoint) {
        LOGGER.warn(
                "event=journey_window_violation motif={} segmentId={} serviceId={} checkpointOrder={}",
                motif,
                segment != null ? segment.getSegmentId() : null,
                segment != null && segment.getService() != null ? segment.getService().getServiceId() : null,
                checkpoint != null ? checkpoint.getOrdre() : null
        );
    }
}
