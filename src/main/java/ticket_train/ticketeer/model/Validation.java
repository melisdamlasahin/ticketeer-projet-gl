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

    @Column(nullable = false)
    private String codeOptique;

    @Column
    private UUID serviceId;

    @ManyToOne
    @JoinColumn(name = "controleur_id", nullable = false)
    private Controleur controleur;

    @ManyToOne
    @JoinColumn(name = "segment_id")
    private SegmentBillet segment;

    public Validation() {}

    public Validation(
            ValidationResult resultat,
            ValidationMotif motif,
            String codeOptique,
            UUID serviceId,
            Controleur controleur,
            SegmentBillet segment
    ) {
        this.timestampControle = LocalDateTime.now();
        this.resultat = resultat;
        this.motif = motif;
        this.codeOptique = codeOptique;
        this.serviceId = serviceId;
        this.controleur = controleur;
        this.segment = segment;
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
    public String getCodeOptique() { return codeOptique; }
    public void setCodeOptique(String codeOptique) { this.codeOptique = codeOptique; }
    public UUID getServiceId() { return serviceId; }
    public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
    public Controleur getControleur() { return controleur; }
    public void setControleur(Controleur controleur) { this.controleur = controleur; }
    public SegmentBillet getSegment() { return segment; }
    public void setSegment(SegmentBillet segment) { this.segment = segment; }
}
