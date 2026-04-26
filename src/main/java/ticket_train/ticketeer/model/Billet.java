package ticket_train.ticketeer.model;

import ticket_train.ticketeer.model.enums.TicketStatus;
import jakarta.persistence.*;
        import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "billets")
public class Billet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID ticketId;

    @Column(nullable = false, unique = true)
    private String codeOptique;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixFinal;

    @Column(nullable = false)
    private LocalDateTime dateEmission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus etat;

    @Column(length = 40)
    private String classeReservation;

    @Column(length = 20)
    private String numeroPlace;

    @Column(length = 120)
    private String nomPassager;

    @Column(length = 160)
    private String emailPassager;

    @Column(length = 40)
    private String telephonePassager;

    @Column(length = 40)
    private String methodePaiement;

    @Column(nullable = false)
    private Boolean confirmationEmailEnvoyee = Boolean.FALSE;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @OneToMany(mappedBy = "billet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SegmentBillet> segments = new ArrayList<>();

    public Billet() {}

    public Billet(String codeOptique, BigDecimal prixFinal, Client client) {
        this.codeOptique = codeOptique;
        this.prixFinal = prixFinal;
        this.client = client;
        this.dateEmission = LocalDateTime.now();
        this.etat = TicketStatus.CREE;
    }

    // Getters et Setters
    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    public String getCodeOptique() { return codeOptique; }
    public void setCodeOptique(String codeOptique) { this.codeOptique = codeOptique; }
    public BigDecimal getPrixFinal() { return prixFinal; }
    public void setPrixFinal(BigDecimal prixFinal) { this.prixFinal = prixFinal; }
    public LocalDateTime getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDateTime dateEmission) { this.dateEmission = dateEmission; }
    public TicketStatus getEtat() { return etat; }
    public void setEtat(TicketStatus etat) { this.etat = etat; }
    public String getClasseReservation() { return classeReservation; }
    public void setClasseReservation(String classeReservation) { this.classeReservation = classeReservation; }
    public String getNumeroPlace() { return numeroPlace; }
    public void setNumeroPlace(String numeroPlace) { this.numeroPlace = numeroPlace; }
    public String getNomPassager() { return nomPassager; }
    public void setNomPassager(String nomPassager) { this.nomPassager = nomPassager; }
    public String getEmailPassager() { return emailPassager; }
    public void setEmailPassager(String emailPassager) { this.emailPassager = emailPassager; }
    public String getTelephonePassager() { return telephonePassager; }
    public void setTelephonePassager(String telephonePassager) { this.telephonePassager = telephonePassager; }
    public String getMethodePaiement() { return methodePaiement; }
    public void setMethodePaiement(String methodePaiement) { this.methodePaiement = methodePaiement; }
    public Boolean getConfirmationEmailEnvoyee() { return confirmationEmailEnvoyee; }
    public void setConfirmationEmailEnvoyee(Boolean confirmationEmailEnvoyee) { this.confirmationEmailEnvoyee = confirmationEmailEnvoyee; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public List<SegmentBillet> getSegments() { return segments; }
    public void setSegments(List<SegmentBillet> segments) { this.segments = segments; }
}
