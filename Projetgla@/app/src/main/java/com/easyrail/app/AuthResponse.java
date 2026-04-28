package com.easyrail.app;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("clientId")
    private String clientId;

    @SerializedName("authToken")
    private String authToken;

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

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getClientId() {
        return clientId;
    }

    public String getAuthToken() {
        return authToken;
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