package ticket_train.ticketeer.model;

import ticket_train.ticketeer.model.enums.ValidationMotif;
import ticket_train.ticketeer.model.enums.ValidationResult;
import jakarta.persistence.*;
        import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "validations")
public class Validation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID validationId;

    @Column(nullable = false)
    private LocalDateTime timestampControle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationResult resultat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationMotif motif;

    @ManyToOne
    @JoinColumn(name = "controleur_id", nullable = false)
    private Controleur controleur;

    @ManyToOne
    @JoinColumn(name = "segment_id", nullable = false)
    private SegmentBillet segment;

    @Column
    private Integer checkpointOrder;

    public Validation() {}

    public Validation(ValidationResult resultat, ValidationMotif motif, Controleur controleur, SegmentBillet segment) {
        this(resultat, motif, controleur, segment, null);
    }

    public Validation(ValidationResult resultat,
                      ValidationMotif motif,
                      Controleur controleur,
                      SegmentBillet segment,
                      Integer checkpointOrder) {
        this.timestampControle = LocalDateTime.now();
        this.resultat = resultat;
        this.motif = motif;
        this.controleur = controleur;
        this.segment = segment;
        this.checkpointOrder = checkpointOrder;
    }

    // Getters et Setters
    public UUID getValidationId() { return validationId; }
    public void setValidationId(UUID validationId) { this.validationId = validationId; }
    public LocalDateTime getTimestampControle() { return timestampControle; }
    public void setTimestampControle(LocalDateTime timestampControle) { this.timestampControle = timestampControle; }
    public ValidationResult getResultat() { return resultat; }
    public void setResultat(ValidationResult resultat) { this.resultat = resultat; }
    public ValidationMotif getMotif() { return motif; }
    public void setMotif(ValidationMotif motif) { this.motif = motif; }
    public Controleur getControleur() { return controleur; }
    public void setControleur(Controleur controleur) { this.controleur = controleur; }
    public SegmentBillet getSegment() { return segment; }
    public void setSegment(SegmentBillet segment) { this.segment = segment; }
    public Integer getCheckpointOrder() { return checkpointOrder; }
    public void setCheckpointOrder(Integer checkpointOrder) { this.checkpointOrder = checkpointOrder; }
}
