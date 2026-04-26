package ticket_train.ticketeer.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clients")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID clientId;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(unique = true)
    private String email;

    @Column
    private String hashMotDePasse;

    @Column
    private String sexe;

    @Column
    private LocalDate dateNaissance;

    @Column
    private String telephone;

    @Column(nullable = false)
    private String photoRef;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Billet> billets = new ArrayList<>();

    public Client() {}

    public Client(String nom, String prenom, String email, String hashMotDePasse, String photoRef) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.hashMotDePasse = hashMotDePasse;
        this.photoRef = photoRef;
    }

    // Getters et Setters
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getHashMotDePasse() { return hashMotDePasse; }
    public void setHashMotDePasse(String hashMotDePasse) { this.hashMotDePasse = hashMotDePasse; }
    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getPhotoRef() { return photoRef; }
    public void setPhotoRef(String photoRef) { this.photoRef = photoRef; }
    public List<Billet> getBillets() { return billets; }
    public void setBillets(List<Billet> billets) { this.billets = billets; }
}
