package ticket_train.ticketeer.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "controleurs")
public class Controleur {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID controleurId;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false)
    private String hashMotDePasse;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    public Controleur() {}

    public Controleur(String login, String hashMotDePasse, String nom, String prenom) {
        this.login = login;
        this.hashMotDePasse = hashMotDePasse;
        this.nom = nom;
        this.prenom = prenom;
    }

    // Getters et Setters
    public UUID getControleurId() { return controleurId; }
    public void setControleurId(UUID controleurId) { this.controleurId = controleurId; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getHashMotDePasse() { return hashMotDePasse; }
    public void setHashMotDePasse(String hashMotDePasse) { this.hashMotDePasse = hashMotDePasse; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
}