package ticket_train.ticketeer.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "service_checkpoints", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"service_id", "ordre"}),
        @UniqueConstraint(columnNames = {"service_id", "ville_id"})
})
public class ServiceCheckpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID checkpointId;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceFerroviaire service;

    @ManyToOne
    @JoinColumn(name = "ville_id", nullable = false)
    private Ville ville;

    @Column(nullable = false)
    private Integer ordre;

    public ServiceCheckpoint() {
    }

    public ServiceCheckpoint(ServiceFerroviaire service, Ville ville, Integer ordre) {
        this.service = service;
        this.ville = ville;
        this.ordre = ordre;
    }

    public UUID getCheckpointId() { return checkpointId; }
    public void setCheckpointId(UUID checkpointId) { this.checkpointId = checkpointId; }
    public ServiceFerroviaire getService() { return service; }
    public void setService(ServiceFerroviaire service) { this.service = service; }
    public Ville getVille() { return ville; }
    public void setVille(Ville ville) { this.ville = ville; }
    public Integer getOrdre() { return ordre; }
    public void setOrdre(Integer ordre) { this.ordre = ordre; }
}
