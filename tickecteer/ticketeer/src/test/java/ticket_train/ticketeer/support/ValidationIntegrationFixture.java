package ticket_train.ticketeer.support;

import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceCheckpoint;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.model.Train;
import ticket_train.ticketeer.model.Ville;
import ticket_train.ticketeer.model.enums.SegmentStatus;
import ticket_train.ticketeer.model.enums.TicketStatus;
import ticket_train.ticketeer.repository.BilletRepository;
import ticket_train.ticketeer.repository.ClientRepository;
import ticket_train.ticketeer.repository.ControleurRepository;
import ticket_train.ticketeer.repository.ServiceCheckpointRepository;
import ticket_train.ticketeer.repository.ServiceFerroviaireRepository;
import ticket_train.ticketeer.repository.TrainRepository;
import ticket_train.ticketeer.repository.VilleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class ValidationIntegrationFixture {

    private final TrainRepository trainRepository;
    private final VilleRepository villeRepository;
    private final ServiceFerroviaireRepository serviceFerroviaireRepository;
    private final ServiceCheckpointRepository serviceCheckpointRepository;
    private final ClientRepository clientRepository;
    private final ControleurRepository controleurRepository;
    private final BilletRepository billetRepository;

    public ValidationIntegrationFixture(TrainRepository trainRepository,
                                        VilleRepository villeRepository,
                                        ServiceFerroviaireRepository serviceFerroviaireRepository,
                                        ServiceCheckpointRepository serviceCheckpointRepository,
                                        ClientRepository clientRepository,
                                        ControleurRepository controleurRepository,
                                        BilletRepository billetRepository) {
        this.trainRepository = trainRepository;
        this.villeRepository = villeRepository;
        this.serviceFerroviaireRepository = serviceFerroviaireRepository;
        this.serviceCheckpointRepository = serviceCheckpointRepository;
        this.clientRepository = clientRepository;
        this.controleurRepository = controleurRepository;
        this.billetRepository = billetRepository;
    }

    public FixtureData createValidationCase(String codeOptique) {
        String suffix = UUID.randomUUID().toString();
        Train train = trainRepository.save(new Train("T-" + suffix, "TGV Test"));
        Ville depart = villeRepository.save(new Ville("Paris-" + suffix));
        Ville arrivee = villeRepository.save(new Ville("Lyon-" + suffix));

        ServiceFerroviaire service = serviceFerroviaireRepository.save(new ServiceFerroviaire(
                LocalDate.now(),
                LocalTime.now().minusMinutes(30),
                train,
                depart,
                arrivee,
                50.0
        ));
        ServiceCheckpoint departureCheckpoint = serviceCheckpointRepository.save(new ServiceCheckpoint(service, depart, 1));
        ServiceCheckpoint arrivalCheckpoint = serviceCheckpointRepository.save(new ServiceCheckpoint(service, arrivee, 2));
        service.setCheckpoints(List.of(departureCheckpoint, arrivalCheckpoint));

        Client client = clientRepository.save(new Client(
                "Dupont",
                "Jean",
                "client-" + suffix + "@test",
                "hash",
                "photo"
        ));
        Controleur controleur = controleurRepository.save(new Controleur("ctrl-" + suffix, "hash", "Nom", "Prenom"));

        Billet billet = new Billet(codeOptique, BigDecimal.TEN, client);
        billet.setEtat(TicketStatus.DISPONIBLE);
        SegmentBillet segment = new SegmentBillet(1, service);
        segment.setBillet(billet);
        segment.setEtatSegment(SegmentStatus.PREVU);
        segment.setOrdreDepartValide(1);
        segment.setOrdreArriveeValide(2);
        billet.setSegments(List.of(segment));
        billet = billetRepository.saveAndFlush(billet);

        return new FixtureData(billet, billet.getSegments().get(0), service, departureCheckpoint, arrivalCheckpoint, controleur);
    }

    public record FixtureData(Billet billet,
                              SegmentBillet segment,
                              ServiceFerroviaire service,
                              ServiceCheckpoint departureCheckpoint,
                              ServiceCheckpoint arrivalCheckpoint,
                              Controleur controleur) {
    }
}
