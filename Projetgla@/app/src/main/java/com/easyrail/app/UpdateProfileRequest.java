package com.easyrail.app;

public class UpdateProfileRequest {

    private String nom;
    private String prenom;
    private String sexe;
    private String dateNaissance;
    private String telephone;

    public UpdateProfileRequest(String nom, String prenom, String sexe,
                                String dateNaissance, String telephone) {
        this.nom = nom;
        this.prenom = prenom;
        this.sexe = sexe;
        this.dateNaissance = dateNaissance;
        this.telephone = telephone;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getSexe() {
        return sexe;
    }

    public String getDateNaissance() {
        return dateNaissance;
    }

    public String getTelephone() {
        return telephone;
    }
}