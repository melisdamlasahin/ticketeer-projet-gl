package ticket_train.ticketeer.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ValidationRequest {
    private String codeOptique;
    private UUID serviceId;
    private LocalDateTime timestamp;

    public String getCodeOptique() {
        return codeOptique;
    }

    public void setCodeOptique(String codeOptique) {
        this.codeOptique = codeOptique;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
