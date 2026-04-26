package ticket_train.ticketeer.dto.mobile;

public class TicketResponse {
    private String billetId;
    private String codeOptique;
    private Double prixFinal;
    private String etat;
    private String dateEmission;
    private String trainNom;
    private String villeDepartNom;
    private String villeArriveeNom;
    private String dateTrajet;
    private String heureDepart;
    private String typeTrajet;
    private String trajetResume;
    private String trainRetourNom;
    private String villeRetourDepartNom;
    private String villeRetourArriveeNom;
    private String dateRetour;
    private String heureRetour;
    private String qrCodeBase64;
    private String voie;
    private Integer retardMinutes;
    private String classeReservation;
    private String numeroPlace;
    private String nomPassager;
    private String emailPassager;
    private String telephonePassager;
    private String methodePaiement;
    private Boolean confirmationEmailEnvoyee;

    public String getBilletId() {
        return billetId;
    }

    public void setBilletId(String billetId) {
        this.billetId = billetId;
    }

    public String getCodeOptique() {
        return codeOptique;
    }

    public void setCodeOptique(String codeOptique) {
        this.codeOptique = codeOptique;
    }

    public Double getPrixFinal() {
        return prixFinal;
    }

    public void setPrixFinal(Double prixFinal) {
        this.prixFinal = prixFinal;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public String getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(String dateEmission) {
        this.dateEmission = dateEmission;
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

    public String getTypeTrajet() {
        return typeTrajet;
    }

    public void setTypeTrajet(String typeTrajet) {
        this.typeTrajet = typeTrajet;
    }

    public String getTrajetResume() {
        return trajetResume;
    }

    public void setTrajetResume(String trajetResume) {
        this.trajetResume = trajetResume;
    }

    public String getTrainRetourNom() {
        return trainRetourNom;
    }

    public void setTrainRetourNom(String trainRetourNom) {
        this.trainRetourNom = trainRetourNom;
    }

    public String getVilleRetourDepartNom() {
        return villeRetourDepartNom;
    }

    public void setVilleRetourDepartNom(String villeRetourDepartNom) {
        this.villeRetourDepartNom = villeRetourDepartNom;
    }

    public String getVilleRetourArriveeNom() {
        return villeRetourArriveeNom;
    }

    public void setVilleRetourArriveeNom(String villeRetourArriveeNom) {
        this.villeRetourArriveeNom = villeRetourArriveeNom;
    }

    public String getDateRetour() {
        return dateRetour;
    }

    public void setDateRetour(String dateRetour) {
        this.dateRetour = dateRetour;
    }

    public String getHeureRetour() {
        return heureRetour;
    }

    public void setHeureRetour(String heureRetour) {
        this.heureRetour = heureRetour;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }

    public void setQrCodeBase64(String qrCodeBase64) {
        this.qrCodeBase64 = qrCodeBase64;
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

    public String getClasseReservation() {
        return classeReservation;
    }

    public void setClasseReservation(String classeReservation) {
        this.classeReservation = classeReservation;
    }

    public String getNumeroPlace() {
        return numeroPlace;
    }

    public void setNumeroPlace(String numeroPlace) {
        this.numeroPlace = numeroPlace;
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

    public Boolean getConfirmationEmailEnvoyee() {
        return confirmationEmailEnvoyee;
    }

    public void setConfirmationEmailEnvoyee(Boolean confirmationEmailEnvoyee) {
        this.confirmationEmailEnvoyee = confirmationEmailEnvoyee;
    }
}
