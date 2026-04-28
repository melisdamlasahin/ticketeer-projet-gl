package com.easyrail.app;

public class ServiceApiModel {

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

    public String getDateTrajet() {
        return dateTrajet;
    }

    public String getHeureDepart() {
        return heureDepart;
    }

    public double getPrixBase() {
        return prixBase;
    }

    public String getTrainNom() {
        return trainNom;
    }

    public String getVilleDepartNom() {
        return villeDepartNom;
    }

    public String getVilleArriveeNom() {
        return villeArriveeNom;
    }

    public String getVoie() {
        return voie;
    }

    public Integer getRetardMinutes() {
        return retardMinutes;
    }
}
