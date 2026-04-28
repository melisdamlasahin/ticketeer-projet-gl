package ticket_train.ticketeer.dto.mobile;

public class AchatBilletResponse {
    private Boolean success;
    private String message;
    private String billetId;
    private Double prixFinal;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBilletId() {
        return billetId;
    }

    public void setBilletId(String billetId) {
        this.billetId = billetId;
    }

    public Double getPrixFinal() {
        return prixFinal;
    }

    public void setPrixFinal(Double prixFinal) {
        this.prixFinal = prixFinal;
    }
}
