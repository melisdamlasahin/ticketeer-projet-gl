package ticket_train.ticketeer.dto;

import java.util.UUID;

public class ValidationRequest {
    private String codeOptique;
    private UUID serviceId;
    private UUID checkpointId;

    public ValidationRequest() {}

    public ValidationRequest(String codeOptique, UUID serviceId) {
        this(codeOptique, serviceId, null);
    }

    public ValidationRequest(String codeOptique, UUID serviceId, UUID checkpointId) {
        this.codeOptique = codeOptique;
        this.serviceId = serviceId;
        this.checkpointId = checkpointId;
    }

    public String getCodeOptique() { return codeOptique; }
    public void setCodeOptique(String codeOptique) { this.codeOptique = codeOptique; }
    public UUID getServiceId() { return serviceId; }
    public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
    public UUID getCheckpointId() { return checkpointId; }
    public void setCheckpointId(UUID checkpointId) { this.checkpointId = checkpointId; }
}
