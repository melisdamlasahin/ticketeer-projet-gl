package ticket_train.ticketeer.model;

import jakarta.persistence.*;
        import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "services_ferroviaires")
public class ServiceFerroviaire {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID serviceId;

    @Column(nullable = false)
    private LocalDate dateTrajet;

    @ManyToOne
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @ManyToOne
    @JoinColumn(name = "ville_depart_id", nullable = false)
    private Ville villeDepart;

    @ManyToOne
    @JoinColumn(name = "ville_arrivee_id", nullable = false)
    private Ville villeArrivee;

    @Column(nullable = false)
    private Double prixBase;

    public ServiceFerroviaire() {}

    public ServiceFerroviaire(LocalDate dateTrajet, Train train, Ville villeDepart, Ville villeArrivee, Double prixBase) {
        this.dateTrajet = dateTrajet;
        this.train = train;
        this.villeDepart = villeDepart;
        this.villeArrivee = villeArrivee;
        this.prixBase = prixBase;
    }

    // Getters et Setters
    public UUID getServiceId() { return serviceId; }
    public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
    public LocalDate getDateTrajet() { return dateTrajet; }
    public void setDateTrajet(LocalDate dateTrajet) { this.dateTrajet = dateTrajet; }
    public Train getTrain() { return train; }
    public void setTrain(Train train) { this.train = train; }
    public Ville getVilleDepart() { return villeDepart; }
    public void setVilleDepart(Ville villeDepart) { this.villeDepart = villeDepart; }
    public Ville getVilleArrivee() { return villeArrivee; }
    public void setVilleArrivee(Ville villeArrivee) { this.villeArrivee = villeArrivee; }
    public Double getPrixBase() { return prixBase; }
    public void setPrixBase(Double prixBase) { this.prixBase = prixBase; }
}
