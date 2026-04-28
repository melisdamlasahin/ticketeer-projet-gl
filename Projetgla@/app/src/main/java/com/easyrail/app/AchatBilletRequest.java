package com.easyrail.app;

public class AchatBilletRequest {

    private String serviceId;
    private String returnServiceId;
    private String profilTarifaire;
    private String clientId;
    private String classeReservation;
    private String preferencePlace;
    private String nomPassager;
    private String emailPassager;
    private String telephonePassager;
    private String methodePaiement;

    public AchatBilletRequest(String serviceId, String profilTarifaire, String clientId) {
        this(serviceId, null, profilTarifaire, clientId);
    }

    public AchatBilletRequest(String serviceId, String returnServiceId, String profilTarifaire, String clientId) {
        this(serviceId, returnServiceId, profilTarifaire, clientId, "SECONDE", null, null, null, null, "CARTE");
    }

    public AchatBilletRequest(String serviceId,
                              String returnServiceId,
                              String profilTarifaire,
                              String clientId,
                              String classeReservation,
                              String preferencePlace,
                              String nomPassager,
                              String emailPassager,
                              String telephonePassager,
                              String methodePaiement) {
        this.serviceId = serviceId;
        this.returnServiceId = returnServiceId;
        this.profilTarifaire = profilTarifaire;
        this.clientId = clientId;
        this.classeReservation = classeReservation;
        this.preferencePlace = preferencePlace;
        this.nomPassager = nomPassager;
        this.emailPassager = emailPassager;
        this.telephonePassager = telephonePassager;
        this.methodePaiement = methodePaiement;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getProfilTarifaire() {
        return profilTarifaire;
    }

    public String getClientId() {
        return clientId;
    }

    public String getReturnServiceId() {
        return returnServiceId;
    }

    public String getClasseReservation() {
        return classeReservation;
    }

    public String getPreferencePlace() {
        return preferencePlace;
    }

    public String getNomPassager() {
        return nomPassager;
    }

    public String getEmailPassager() {
        return emailPassager;
    }

    public String getTelephonePassager() {
        return telephonePassager;
    }

    public String getMethodePaiement() {
        return methodePaiement;
    }
}
