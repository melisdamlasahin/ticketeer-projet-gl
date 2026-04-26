package ticket_train.ticketeer.dto.mobile;

public class ServiceResponse {
    private String serviceId;
    private String dateTrajet;
    private String heureDepart;
    private double prixBase;
    private String trainNom;
    private String villeDepartNom;
    private String villeArriveeNom;
    private String voie;
    private Integer retardMinutes;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getDateTrajet() {
        return dateTrajet;
    }

    public void setDateTrajet(String dateTrajet) {
        this.dateTrajet = dateTrajet;
    }

    public String getHeureDepart() {
        return heureDepart;
    }

    public void setHeureDepart(String heureDepart) {
        this.heureDepart = heureDepart;
    }

    public double getPrixBase() {
        return prixBase;
    }

    public void setPrixBase(double prixBase) {
        this.prixBase = prixBase;
    }

    public String getTrainNom() {
        return trainNom;
    }

    public void setTrainNom(String trainNom) {
        this.trainNom = trainNom;
    }

    public String getVilleDepartNom() {
        return villeDepartNom;
    }

    public void setVilleDepartNom(String villeDepartNom) {
        this.villeDepartNom = villeDepartNom;
    }

    public String getVilleArriveeNom() {
        return villeArriveeNom;
    }

    public void setVilleArriveeNom(String villeArriveeNom) {
        this.villeArriveeNom = villeArriveeNom;
    }

    public String getVoie() {
        return voie;
    }

    public void setVoie(String voie) {
        this.voie = voie;
    }

    public Integer getRetardMinutes() {
        return retardMinutes;
    }

    public void setRetardMinutes(Integer retardMinutes) {
        this.retardMinutes = retardMinutes;
    }
}
