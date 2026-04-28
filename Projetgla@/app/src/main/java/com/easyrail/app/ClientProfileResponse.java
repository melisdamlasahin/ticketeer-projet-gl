package com.easyrail.app;

import com.google.gson.annotations.SerializedName;

public class ClientProfileResponse {

    @SerializedName("clientId")
    private String clientId;

    @SerializedName("nom")
    private String nom;

    @SerializedName("prenom")
    private String prenom;

    @SerializedName("email")
    private String email;

    @SerializedName("sexe")
    private String sexe;

    @SerializedName("dateNaissance")
    private String dateNaissance;

    @SerializedName("telephone")
    private String telephone;

    public String getClientId() {
        return clientId;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getEmail() {
        return email;
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