package ticket_train.ticketeer.dto.mobile;

import jakarta.validation.constraints.Pattern;

public class UpdateProfileRequest {
    private String nom;
    private String prenom;
    private String sexe;
    private String dateNaissance;

    @Pattern(
            regexp = "^$|^[0-9+() .-]{6,20}$",
            message = "Le numero de telephone doit etre valide"
    )
    private String telephone;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public String getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(String dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
}
