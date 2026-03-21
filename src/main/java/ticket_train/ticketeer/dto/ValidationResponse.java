package ticket_train.ticketeer.dto;

import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;

import java.time.LocalDateTime;
import java.util.UUID;

public class ValidationResponse {
    private UUID validationId;
    private UUID ticketId;
    private UUID segmentId;
    private UUID serviceId;
    private String controllerLogin;
    private ValidationResult resultat;
    private ValidationMotif motif;
    private LocalDateTime timestampControle;

    public UUID getValidationId() {
        return validationId;
    }

    public void setValidationId(UUID validationId) {
        this.validationId = validationId;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public UUID getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(UUID segmentId) {
        this.segmentId = segmentId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public String getControllerLogin() {
        return controllerLogin;
    }

    public void setControllerLogin(String controllerLogin) {
        this.controllerLogin = controllerLogin;
    }

    public ValidationResult getResultat() {
        return resultat;
    }

    public void setResultat(ValidationResult resultat) {
        this.resultat = resultat;
    }

    public ValidationMotif getMotif() {
        return motif;
    }

    public void setMotif(ValidationMotif motif) {
        this.motif = motif;
    }

    public LocalDateTime getTimestampControle() {
        return timestampControle;
    }

    public void setTimestampControle(LocalDateTime timestampControle) {
        this.timestampControle = timestampControle;
    }
}
