package ticket_train.ticketeer.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "services_ferroviaires")
public class ServiceFerroviaire {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID serviceId;

    @Column(nullable = false)
    private LocalDate dateTrajet;

    private LocalTime heureDepart;

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

    @Column(length = 20)
    private String voie;

    @Column(nullable = false)
    private Integer retardMinutes = 0;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceCheckpoint> checkpoints = new ArrayList<>();

    public ServiceFerroviaire() {}

    public ServiceFerroviaire(LocalDate dateTrajet, Train train, Ville villeDepart, Ville villeArrivee, Double prixBase) {
        this(dateTrajet, null, train, villeDepart, villeArrivee, prixBase);
    }

    public ServiceFerroviaire(LocalDate dateTrajet, LocalTime heureDepart, Train train, Ville villeDepart, Ville villeArrivee, Double prixBase) {
        this.dateTrajet = dateTrajet;
        this.heureDepart = heureDepart;
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
    public LocalTime getHeureDepart() { return heureDepart; }
    public void setHeureDepart(LocalTime heureDepart) { this.heureDepart = heureDepart; }
    public Train getTrain() { return train; }
    public void setTrain(Train train) { this.train = train; }
    public Ville getVilleDepart() { return villeDepart; }
    public void setVilleDepart(Ville villeDepart) { this.villeDepart = villeDepart; }
    public Ville getVilleArrivee() { return villeArrivee; }
    public void setVilleArrivee(Ville villeArrivee) { this.villeArrivee = villeArrivee; }
    public Double getPrixBase() { return prixBase; }
    public void setPrixBase(Double prixBase) { this.prixBase = prixBase; }
    public String getVoie() { return voie; }
    public void setVoie(String voie) { this.voie = voie; }
    public Integer getRetardMinutes() { return retardMinutes; }
    public void setRetardMinutes(Integer retardMinutes) { this.retardMinutes = retardMinutes; }
    public List<ServiceCheckpoint> getCheckpoints() { return checkpoints; }
    public void setCheckpoints(List<ServiceCheckpoint> checkpoints) { this.checkpoints = checkpoints; }
}
