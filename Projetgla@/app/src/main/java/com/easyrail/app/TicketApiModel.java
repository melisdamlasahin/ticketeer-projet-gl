package com.easyrail.app;

public class TicketApiModel {

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

    public String getBilletId() { return billetId; }
    public String getCodeOptique() { return codeOptique; }
    public Double getPrixFinal() { return prixFinal; }
    public String getEtat() { return etat; }
    public String getDateEmission() { return dateEmission; }
    public String getTrainNom() { return trainNom; }
    public String getVilleDepartNom() { return villeDepartNom; }
    public String getVilleArriveeNom() { return villeArriveeNom; }
    public String getDateTrajet() { return dateTrajet; }
    public String getHeureDepart() { return heureDepart; }
    public String getTypeTrajet() { return typeTrajet; }
    public String getTrajetResume() { return trajetResume; }
    public String getTrainRetourNom() { return trainRetourNom; }
    public String getVilleRetourDepartNom() { return villeRetourDepartNom; }
    public String getVilleRetourArriveeNom() { return villeRetourArriveeNom; }
    public String getDateRetour() { return dateRetour; }
    public String getHeureRetour() { return heureRetour; }
    public String getQrCodeBase64() { return qrCodeBase64; }
    public String getVoie() { return voie; }
    public Integer getRetardMinutes() { return retardMinutes; }
    public String getClasseReservation() { return classeReservation; }
    public String getNumeroPlace() { return numeroPlace; }
    public String getNomPassager() { return nomPassager; }
    public String getEmailPassager() { return emailPassager; }
    public String getTelephonePassager() { return telephonePassager; }
    public String getMethodePaiement() { return methodePaiement; }
    public Boolean getConfirmationEmailEnvoyee() { return confirmationEmailEnvoyee; }
}
