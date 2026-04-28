package com.easyrail.app;

public class AchatBilletResponse {

    private Boolean success;
    private String message;
    private String billetId;
    private Double prixFinal;

    public Boolean getSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getBilletId() {
        return billetId;
    }

    public Double getPrixFinal() {
        return prixFinal;
    }
}
