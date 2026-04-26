package ticket_train.ticketeer.model;

import jakarta.persistence.*;
        import java.util.UUID;

@Entity
@Table(name = "villes")
public class Ville {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID villeId;

    @Column(nullable = false, unique = true)
    private String nom;

    public Ville() {}

    public Ville(String nom) {
        this.nom = nom;
    }

    // Getters et Setters
    public UUID getVilleId() { return villeId; }
    public void setVilleId(UUID villeId) { this.villeId = villeId; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
}