package ticket_train.ticketeer.model;

import ticket_train.ticketeer.model.enums.SegmentStatus;
import jakarta.persistence.*;
        import java.util.UUID;

@Entity
@Table(name = "segments_billet", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"billet_id", "ordre"})
})
public class SegmentBillet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID segmentId;

    @Column(nullable = false)
    private Integer ordre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SegmentStatus etatSegment = SegmentStatus.PREVU;

    @ManyToOne
    @JoinColumn(name = "billet_id", nullable = false)
    private Billet billet;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceFerroviaire service;

    @Column(nullable = false)
    private Integer ordreDepartValide = 1;

    @Column(nullable = false)
    private Integer ordreArriveeValide = 2;

    public SegmentBillet() {}

    public SegmentBillet(Integer ordre, ServiceFerroviaire service) {
        this.ordre = ordre;
        this.service = service;
    }

    // Getters et Setters
    public UUID getSegmentId() { return segmentId; }
    public void setSegmentId(UUID segmentId) { this.segmentId = segmentId; }
    public Integer getOrdre() { return ordre; }
    public void setOrdre(Integer ordre) { this.ordre = ordre; }
    public SegmentStatus getEtatSegment() { return etatSegment; }
    public void setEtatSegment(SegmentStatus etatSegment) { this.etatSegment = etatSegment; }
    public Billet getBillet() { return billet; }
    public void setBillet(Billet billet) { this.billet = billet; }
    public ServiceFerroviaire getService() { return service; }
    public void setService(ServiceFerroviaire service) { this.service = service; }
    public Integer getOrdreDepartValide() { return ordreDepartValide; }
    public void setOrdreDepartValide(Integer ordreDepartValide) { this.ordreDepartValide = ordreDepartValide; }
    public Integer getOrdreArriveeValide() { return ordreArriveeValide; }
    public void setOrdreArriveeValide(Integer ordreArriveeValide) { this.ordreArriveeValide = ordreArriveeValide; }
}
