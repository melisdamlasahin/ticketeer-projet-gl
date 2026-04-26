package ticket_train.ticketeer.dto;

import java.util.UUID;

public class ScanBilletForm {
    private String codeOptique;
    private UUID serviceId;

    public ScanBilletForm() {}

    public String getCodeOptique() { return codeOptique; }
    public void setCodeOptique(String codeOptique) { this.codeOptique = codeOptique; }
    public UUID getServiceId() { return serviceId; }
    public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
}
