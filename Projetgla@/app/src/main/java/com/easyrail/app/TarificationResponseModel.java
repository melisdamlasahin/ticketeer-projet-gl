package com.easyrail.app;

public class TarificationResponseModel {

    private String serviceId;
    private String profilTarifaire;
    private Double prixBase;
    private Double prixFinal;

    public String getServiceId() {
        return serviceId;
    }

    public String getProfilTarifaire() {
        return profilTarifaire;
    }

    public Double getPrixBase() {
        return prixBase;
    }

    public Double getPrixFinal() {
        return prixFinal;
    }
}