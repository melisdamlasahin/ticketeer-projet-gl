package ticket_train.ticketeer.model;

import jakarta.persistence.*;
        import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trains")
public class Train {
    @Id
    private String trainId;

    @Column(nullable = false)
    private String nomTrain;

    @OneToMany(mappedBy = "train")
    private List<ServiceFerroviaire> services = new ArrayList<>();

    public Train() {}

    public Train(String trainId, String nomTrain) {
        this.trainId = trainId;
        this.nomTrain = nomTrain;
    }

    // Getters et Setters
    public String getTrainId() { return trainId; }
    public void setTrainId(String trainId) { this.trainId = trainId; }
    public String getNomTrain() { return nomTrain; }
    public void setNomTrain(String nomTrain) { this.nomTrain = nomTrain; }
    public List<ServiceFerroviaire> getServices() { return services; }
    public void setServices(List<ServiceFerroviaire> services) { this.services = services; }
}
