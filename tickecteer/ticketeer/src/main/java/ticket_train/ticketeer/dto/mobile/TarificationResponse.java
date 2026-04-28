package ticket_train.ticketeer.dto.mobile;

public class TarificationResponse {
    private String serviceId;
    private String profilTarifaire;
    private Double prixBase;
    private Double prixFinal;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getProfilTarifaire() {
        return profilTarifaire;
    }

    public void setProfilTarifaire(String profilTarifaire) {
        this.profilTarifaire = profilTarifaire;
    }

    public Double getPrixBase() {
        return prixBase;
    }

    public void setPrixBase(Double prixBase) {
        this.prixBase = prixBase;
    }

    public Double getPrixFinal() {
        return prixFinal;
    }

    public void setPrixFinal(Double prixFinal) {
        this.prixFinal = prixFinal;
    }
}
