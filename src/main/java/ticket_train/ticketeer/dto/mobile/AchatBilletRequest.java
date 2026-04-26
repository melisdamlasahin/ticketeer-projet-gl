package ticket_train.ticketeer.dto.mobile;

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

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getReturnServiceId() {
        return returnServiceId;
    }

    public void setReturnServiceId(String returnServiceId) {
        this.returnServiceId = returnServiceId;
    }

    public String getProfilTarifaire() {
        return profilTarifaire;
    }

    public void setProfilTarifaire(String profilTarifaire) {
        this.profilTarifaire = profilTarifaire;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClasseReservation() {
        return classeReservation;
    }

    public void setClasseReservation(String classeReservation) {
        this.classeReservation = classeReservation;
    }

    public String getPreferencePlace() {
        return preferencePlace;
    }

    public void setPreferencePlace(String preferencePlace) {
        this.preferencePlace = preferencePlace;
    }

    public String getNomPassager() {
        return nomPassager;
    }

    public void setNomPassager(String nomPassager) {
        this.nomPassager = nomPassager;
    }

    public String getEmailPassager() {
        return emailPassager;
    }

    public void setEmailPassager(String emailPassager) {
        this.emailPassager = emailPassager;
    }

    public String getTelephonePassager() {
        return telephonePassager;
    }

    public void setTelephonePassager(String telephonePassager) {
        this.telephonePassager = telephonePassager;
    }

    public String getMethodePaiement() {
        return methodePaiement;
    }

    public void setMethodePaiement(String methodePaiement) {
        this.methodePaiement = methodePaiement;
    }
}
