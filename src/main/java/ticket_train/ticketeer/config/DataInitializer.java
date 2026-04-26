package ticket_train.ticketeer.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ticket_train.ticketeer.model.Billet;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.model.SegmentBillet;
import ticket_train.ticketeer.model.ServiceCheckpoint;
import ticket_train.ticketeer.model.ServiceFerroviaire;
import ticket_train.ticketeer.model.Train;
import ticket_train.ticketeer.model.Ville;
import ticket_train.ticketeer.model.enums.TicketStatus;
import ticket_train.ticketeer.repository.BilletRepository;
import ticket_train.ticketeer.repository.ClientRepository;
import ticket_train.ticketeer.repository.ControleurRepository;
import ticket_train.ticketeer.repository.SegmentBilletRepository;
import ticket_train.ticketeer.repository.ServiceCheckpointRepository;
import ticket_train.ticketeer.repository.ServiceFerroviaireRepository;
import ticket_train.ticketeer.repository.TrainRepository;
import ticket_train.ticketeer.repository.VilleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ControleurRepository controleurRepository;
    private final ClientRepository clientRepository;
    private final VilleRepository villeRepository;
    private final TrainRepository trainRepository;
    private final ServiceFerroviaireRepository serviceFerroviaireRepository;
    private final ServiceCheckpointRepository serviceCheckpointRepository;
    private final BilletRepository billetRepository;
    private final SegmentBilletRepository segmentBilletRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            ControleurRepository controleurRepository,
            ClientRepository clientRepository,
            VilleRepository villeRepository,
            TrainRepository trainRepository,
            ServiceFerroviaireRepository serviceFerroviaireRepository,
            ServiceCheckpointRepository serviceCheckpointRepository,
            BilletRepository billetRepository,
            SegmentBilletRepository segmentBilletRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.controleurRepository = controleurRepository;
        this.clientRepository = clientRepository;
        this.villeRepository = villeRepository;
        this.trainRepository = trainRepository;
        this.serviceFerroviaireRepository = serviceFerroviaireRepository;
        this.serviceCheckpointRepository = serviceCheckpointRepository;
        this.billetRepository = billetRepository;
        this.segmentBilletRepository = segmentBilletRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureControllers();
        ensureClients();
        ensureServices();
        ensureCheckpoints();
        ensureSampleTicket();
        upgradeLegacyPasswords();
    }

    private void ensureControllers() {
        ensureController("nathan", "anne123", "Petit", "Paul");
        ensureController("christian", "essome123", "Robert", "Sophie");
    }

    private void ensureClients() {
        ensureClient("Jean", "Dupont", "jean.dupont@easyrail.test", "password123");
        ensureClient("Marie", "Martin", "marie.martin@easyrail.test", "password123");
    }

    private void ensureServices() {
        Ville paris = ensureVille("Paris");
        Ville lyon = ensureVille("Lyon");
        Ville marseille = ensureVille("Marseille");
        Ville bordeaux = ensureVille("Bordeaux");
        Ville lille = ensureVille("Lille");
        Ville toulouse = ensureVille("Toulouse");
        Ville nantes = ensureVille("Nantes");
        Ville strasbourg = ensureVille("Strasbourg");
        Ville nice = ensureVille("Nice");
        Ville rennes = ensureVille("Rennes");

        Train tgv100 = ensureTrain("TGV100", "TGV Paris-Lyon");
        Train tgv101 = ensureTrain("TGV101", "TGV Lyon-Paris");
        Train tgv200 = ensureTrain("TGV200", "TGV Lyon-Marseille");
        Train tgv201 = ensureTrain("TGV201", "TGV Marseille-Lyon");
        Train inter300 = ensureTrain("INTER300", "Intercités Bordeaux-Paris");
        Train inter301 = ensureTrain("INTER301", "Intercités Paris-Bordeaux");
        Train ter400 = ensureTrain("TER400", "TER Lille-Paris");
        Train ter401 = ensureTrain("TER401", "TER Paris-Lille");
        Train tgv500 = ensureTrain("TGV500", "TGV Paris-Strasbourg");
        Train tgv501 = ensureTrain("TGV501", "TGV Strasbourg-Paris");
        Train inter600 = ensureTrain("INTER600", "Intercités Paris-Nantes");
        Train inter601 = ensureTrain("INTER601", "Intercités Nantes-Paris");
        Train intercite700 = ensureTrain("INTER700", "Intercités Bordeaux-Toulouse");
        Train intercite701 = ensureTrain("INTER701", "Intercités Toulouse-Bordeaux");
        Train tgv800 = ensureTrain("TGV800", "TGV Marseille-Nice");
        Train tgv801 = ensureTrain("TGV801", "TGV Nice-Marseille");
        Train ter900 = ensureTrain("TER900", "TER Rennes-Nantes");
        Train ter901 = ensureTrain("TER901", "TER Nantes-Rennes");
        Train tgv1000 = ensureTrain("TGV1000", "TGV Paris-Marseille");
        Train tgv1001 = ensureTrain("TGV1001", "TGV Marseille-Paris");
        Train tgv1100 = ensureTrain("TGV1100", "TGV Lille-Lyon");
        Train tgv1101 = ensureTrain("TGV1101", "TGV Lyon-Lille");
        Train tgv1200 = ensureTrain("TGV1200", "TGV Paris-Bordeaux");
        Train tgv1201 = ensureTrain("TGV1201", "TGV Bordeaux-Paris");
        Train tgv1300 = ensureTrain("TGV1300", "TGV Paris-Lyon Matin");
        Train tgv1301 = ensureTrain("TGV1301", "TGV Lyon-Paris Matin");
        Train tgv1302 = ensureTrain("TGV1302", "TGV Paris-Lyon Soir");
        Train tgv1303 = ensureTrain("TGV1303", "TGV Lyon-Paris Soir");
        Train tgv1400 = ensureTrain("TGV1400", "TGV Paris-Marseille Matin");
        Train tgv1401 = ensureTrain("TGV1401", "TGV Marseille-Paris Matin");
        Train tgv1402 = ensureTrain("TGV1402", "TGV Paris-Marseille Soir");
        Train tgv1403 = ensureTrain("TGV1403", "TGV Marseille-Paris Soir");
        Train inter1500 = ensureTrain("INTER1500", "Intercités Paris-Bordeaux Matin");
        Train inter1501 = ensureTrain("INTER1501", "Intercités Bordeaux-Paris Matin");
        Train inter1502 = ensureTrain("INTER1502", "Intercités Paris-Bordeaux Soir");
        Train inter1503 = ensureTrain("INTER1503", "Intercités Bordeaux-Paris Soir");
        Train tgv1600 = ensureTrain("TGV1600", "TGV Paris-Strasbourg Matin");
        Train tgv1601 = ensureTrain("TGV1601", "TGV Strasbourg-Paris Matin");
        Train tgv1602 = ensureTrain("TGV1602", "TGV Paris-Strasbourg Soir");
        Train tgv1603 = ensureTrain("TGV1603", "TGV Strasbourg-Paris Soir");
        Train inter1700 = ensureTrain("INTER1700", "Intercités Paris-Nantes Matin");
        Train inter1701 = ensureTrain("INTER1701", "Intercités Nantes-Paris Matin");
        Train inter1702 = ensureTrain("INTER1702", "Intercités Paris-Nantes Soir");
        Train inter1703 = ensureTrain("INTER1703", "Intercités Nantes-Paris Soir");
        Train tgv1800 = ensureTrain("TGV1800", "TGV Lyon-Marseille Matin");
        Train tgv1801 = ensureTrain("TGV1801", "TGV Marseille-Lyon Matin");
        Train tgv1802 = ensureTrain("TGV1802", "TGV Lyon-Marseille Soir");
        Train tgv1803 = ensureTrain("TGV1803", "TGV Marseille-Lyon Soir");

        ensureRoundTripSeries(tgv100, tgv101, paris, lyon, List.of(
                routePlan("2026-03-18", 89.90, "2026-03-21", 84.90),
                routePlan("2026-04-10", 92.50, "2026-04-12", 88.40),
                routePlan("2026-05-03", 86.20, "2026-05-05", 82.70),
                routePlan("2026-05-17", 90.10, "2026-05-20", 87.10)
        ));

        ensureRoundTripSeries(tgv200, tgv201, lyon, marseille, List.of(
                routePlan("2026-03-20", 75.50, "2026-03-23", 73.00),
                routePlan("2026-04-15", 78.00, "2026-04-18", 76.50),
                routePlan("2026-05-09", 79.90, "2026-05-11", 77.80)
        ));

        ensureRoundTripSeries(inter301, inter300, paris, bordeaux, List.of(
                routePlan("2026-03-25", 91.00, "2026-03-28", 95.00),
                routePlan("2026-04-05", 93.20, "2026-04-08", 97.50),
                routePlan("2026-05-14", 94.10, "2026-05-17", 98.40)
        ));

        ensureRoundTripSeries(ter401, ter400, paris, lille, List.of(
                routePlan("2026-04-02", 47.90, "2026-04-04", 49.90),
                routePlan("2026-04-20", 50.00, "2026-04-22", 52.00),
                routePlan("2026-05-12", 48.60, "2026-05-15", 51.20)
        ));

        ensureRoundTripSeries(tgv500, tgv501, paris, strasbourg, List.of(
                routePlan("2026-04-06", 82.00, "2026-04-09", 79.50),
                routePlan("2026-05-02", 85.40, "2026-05-04", 83.00),
                routePlan("2026-05-24", 87.10, "2026-05-27", 84.80)
        ));

        ensureRoundTripSeries(inter600, inter601, paris, nantes, List.of(
                routePlan("2026-04-07", 58.00, "2026-04-10", 56.50),
                routePlan("2026-05-06", 59.40, "2026-05-08", 57.70),
                routePlan("2026-05-22", 61.20, "2026-05-25", 59.90)
        ));

        ensureRoundTripSeries(intercite700, intercite701, bordeaux, toulouse, List.of(
                routePlan("2026-04-11", 44.50, "2026-04-13", 43.20),
                routePlan("2026-05-10", 46.10, "2026-05-12", 44.80),
                routePlan("2026-05-26", 47.00, "2026-05-29", 45.40)
        ));

        ensureRoundTripSeries(tgv800, tgv801, marseille, nice, List.of(
                routePlan("2026-04-16", 39.90, "2026-04-18", 38.80),
                routePlan("2026-05-07", 41.20, "2026-05-09", 40.10),
                routePlan("2026-05-28", 42.00, "2026-05-30", 40.90)
        ));

        ensureRoundTripSeries(ter900, ter901, rennes, nantes, List.of(
                routePlan("2026-04-03", 26.90, "2026-04-05", 25.90),
                routePlan("2026-05-01", 27.40, "2026-05-03", 26.20),
                routePlan("2026-05-19", 28.00, "2026-05-21", 26.90)
        ));

        ensureRoundTripSeries(tgv1000, tgv1001, paris, marseille, List.of(
                routePlan("2026-04-14", 118.00, "2026-04-19", 112.00),
                routePlan("2026-05-13", 121.50, "2026-05-16", 116.90),
                routePlan("2026-05-30", 124.00, "2026-06-02", 118.40)
        ));

        ensureRoundTripSeries(tgv1100, tgv1101, lille, lyon, List.of(
                routePlan("2026-04-17", 72.00, "2026-04-20", 69.80),
                routePlan("2026-05-11", 74.30, "2026-05-14", 71.60),
                routePlan("2026-05-27", 75.10, "2026-05-30", 72.50)
        ));

        ensureRoundTripSeries(tgv1200, tgv1201, paris, bordeaux, List.of(
                routePlan("2026-04-09", 88.50, "2026-04-12", 85.90),
                routePlan("2026-05-04", 90.20, "2026-05-06", 87.40),
                routePlan("2026-05-23", 91.80, "2026-05-26", 88.70)
        ));

        ensureRoundTripSeries(tgv1300, tgv1301, paris, lyon, List.of(
                routePlan("2026-05-03", "07:15", 93.40, "2026-05-05", "08:10", 89.60),
                routePlan("2026-05-17", "07:20", 95.10, "2026-05-20", "08:05", 91.20)
        ));
        ensureRoundTripSeries(tgv1302, tgv1303, paris, lyon, List.of(
                routePlan("2026-05-03", "18:40", 98.20, "2026-05-05", "19:10", 94.80),
                routePlan("2026-05-17", "18:55", 99.50, "2026-05-20", "19:20", 95.90)
        ));

        ensureRoundTripSeries(tgv1400, tgv1401, paris, marseille, List.of(
                routePlan("2026-05-13", "06:50", 126.00, "2026-05-16", "07:30", 119.00),
                routePlan("2026-05-30", "06:45", 128.00, "2026-06-02", "07:20", 121.80)
        ));
        ensureRoundTripSeries(tgv1402, tgv1403, paris, marseille, List.of(
                routePlan("2026-05-13", "17:35", 131.50, "2026-05-16", "18:05", 124.00),
                routePlan("2026-05-30", "17:50", 133.40, "2026-06-02", "18:20", 126.10)
        ));

        ensureRoundTripSeries(inter1500, inter1501, paris, bordeaux, List.of(
                routePlan("2026-05-04", "08:00", 92.60, "2026-05-06", "08:25", 89.10),
                routePlan("2026-05-23", "08:15", 94.20, "2026-05-26", "08:40", 90.40)
        ));
        ensureRoundTripSeries(inter1502, inter1503, paris, bordeaux, List.of(
                routePlan("2026-05-04", "18:10", 96.80, "2026-05-06", "18:35", 93.00),
                routePlan("2026-05-23", "18:20", 98.10, "2026-05-26", "18:45", 94.50)
        ));

        ensureRoundTripSeries(tgv1600, tgv1601, paris, strasbourg, List.of(
                routePlan("2026-05-02", "07:45", 88.20, "2026-05-04", "08:10", 84.70),
                routePlan("2026-05-24", "07:55", 90.10, "2026-05-27", "08:20", 86.50)
        ));
        ensureRoundTripSeries(tgv1602, tgv1603, paris, strasbourg, List.of(
                routePlan("2026-05-02", "19:00", 92.40, "2026-05-04", "19:20", 88.90),
                routePlan("2026-05-24", "18:50", 94.30, "2026-05-27", "19:15", 90.60)
        ));

        ensureRoundTripSeries(inter1700, inter1701, paris, nantes, List.of(
                routePlan("2026-05-06", "08:05", 62.40, "2026-05-08", "08:30", 59.80),
                routePlan("2026-05-22", "08:10", 63.20, "2026-05-25", "08:35", 60.70)
        ));
        ensureRoundTripSeries(inter1702, inter1703, paris, nantes, List.of(
                routePlan("2026-05-06", "18:00", 65.00, "2026-05-08", "18:20", 62.20),
                routePlan("2026-05-22", "18:10", 66.10, "2026-05-25", "18:30", 63.40)
        ));

        ensureRoundTripSeries(tgv1800, tgv1801, lyon, marseille, List.of(
                routePlan("2026-05-09", "07:30", 82.30, "2026-05-11", "08:15", 79.20)
        ));
        ensureRoundTripSeries(tgv1802, tgv1803, lyon, marseille, List.of(
                routePlan("2026-05-09", "18:25", 84.60, "2026-05-11", "19:00", 81.50)
        ));
    }

    private void ensureSampleTicket() {
        Client client = clientRepository.findByEmail("jean.dupont@easyrail.test").orElse(null);
        if (client == null || billetRepository.existsByCodeOptique("EASY-DEMO-001")) {
            return;
        }

        ServiceFerroviaire service = serviceFerroviaireRepository.findAll().stream().findFirst().orElse(null);
        if (service == null) {
            return;
        }

        Billet billet = new Billet("EASY-DEMO-001", BigDecimal.valueOf(service.getPrixBase()), client);
        billet.setEtat(TicketStatus.DISPONIBLE);
        billetRepository.save(billet);

        SegmentBillet segment = new SegmentBillet(1, service);
        segment.setOrdreDepartValide(1);
        segment.setOrdreArriveeValide(serviceCheckpointRepository.findByServiceOrderByOrdreAsc(service).stream()
                .map(ServiceCheckpoint::getOrdre)
                .max(Integer::compareTo)
                .orElse(2));
        segment.setBillet(billet);
        billet.getSegments().add(segment);
        segmentBilletRepository.save(segment);
        billetRepository.save(billet);
    }

    private void ensureCheckpoints() {
        for (ServiceFerroviaire service : serviceFerroviaireRepository.findAll()) {
            if (!serviceCheckpointRepository.findByServiceOrderByOrdreAsc(service).isEmpty()) {
                continue;
            }

            Ville departure = service.getVilleDepart();
            Ville arrival = service.getVilleArrivee();
            String dep = departure.getNom();
            String arr = arrival.getNom();

            ensureCheckpoint(service, departure, 1);
            if ((dep.equals("Paris") && arr.equals("Marseille")) || (dep.equals("Marseille") && arr.equals("Paris"))) {
                Ville lyon = villeRepository.findByNom("Lyon").orElseGet(() -> villeRepository.save(new Ville("Lyon")));
                ensureCheckpoint(service, lyon, 2);
                ensureCheckpoint(service, arrival, 3);
            } else {
                ensureCheckpoint(service, arrival, 2);
            }
        }
    }

    private void ensureCheckpoint(ServiceFerroviaire service, Ville ville, int ordre) {
        ServiceCheckpoint checkpoint = new ServiceCheckpoint(service, ville, ordre);
        serviceCheckpointRepository.save(checkpoint);
    }

    private void upgradeLegacyPasswords() {
        List<Controleur> controleurs = controleurRepository.findAll();
        for (Controleur controleur : controleurs) {
            String current = controleur.getHashMotDePasse();
            if (needsHash(current)) {
                controleur.setHashMotDePasse(passwordEncoder.encode(current));
                controleurRepository.save(controleur);
            }
        }

        List<Client> clients = clientRepository.findAll();
        for (Client client : clients) {
            String current = client.getHashMotDePasse();
            if (needsHash(current)) {
                client.setHashMotDePasse(passwordEncoder.encode(current));
                clientRepository.save(client);
            }
        }
    }

    private boolean needsHash(String value) {
        return value != null && !value.startsWith("$2a$") && !value.startsWith("$2b$");
    }

    private void ensureController(String login, String rawPassword, String nom, String prenom) {
        if (controleurRepository.findByLogin(login).isPresent()) {
            return;
        }
        Controleur controleur = new Controleur(login, passwordEncoder.encode(rawPassword), nom, prenom);
        controleurRepository.save(controleur);
    }

    private void ensureClient(String nom, String prenom, String email, String rawPassword) {
        if (clientRepository.findByEmail(email).isPresent()) {
            return;
        }
        Client client = new Client(nom, prenom, email, passwordEncoder.encode(rawPassword), "photos/default-client.png");
        clientRepository.save(client);
    }

    private Ville ensureVille(String nom) {
        return villeRepository.findByNom(nom).orElseGet(() -> villeRepository.save(new Ville(nom)));
    }

    private Train ensureTrain(String trainId, String nomTrain) {
        return trainRepository.findById(trainId).orElseGet(() -> trainRepository.save(new Train(trainId, nomTrain)));
    }

    private void ensureService(Train train,
                               LocalDate dateTrajet,
                               LocalTime heureDepart,
                               Ville villeDepart,
                               Ville villeArrivee,
                               double prixBase) {
        if (serviceFerroviaireRepository.findByTrainTrainIdAndDateTrajetAndVilleDepartNomAndVilleArriveeNom(
                train.getTrainId(),
                dateTrajet,
                villeDepart.getNom(),
                villeArrivee.getNom()
        ).isPresent()) {
            return;
        }
        serviceFerroviaireRepository.save(new ServiceFerroviaire(dateTrajet, heureDepart, train, villeDepart, villeArrivee, prixBase));
    }

    private void ensureRoundTripSeries(Train outboundTrain,
                                       Train returnTrain,
                                       Ville outboundDeparture,
                                       Ville outboundArrival,
                                       List<RoutePlan> plans) {
        for (RoutePlan plan : plans) {
            ensureService(outboundTrain, plan.outboundDate(), plan.outboundTime(), outboundDeparture, outboundArrival, plan.outboundPrice());
            ensureService(returnTrain, plan.returnDate(), plan.returnTime(), outboundArrival, outboundDeparture, plan.returnPrice());
        }
    }

    private RoutePlan routePlan(String outboundDate, double outboundPrice, String returnDate, double returnPrice) {
        return routePlan(outboundDate, "09:00", outboundPrice, returnDate, "17:00", returnPrice);
    }

    private RoutePlan routePlan(String outboundDate,
                                String outboundTime,
                                double outboundPrice,
                                String returnDate,
                                String returnTime,
                                double returnPrice) {
        return new RoutePlan(
                LocalDate.parse(outboundDate),
                LocalTime.parse(outboundTime),
                outboundPrice,
                LocalDate.parse(returnDate),
                LocalTime.parse(returnTime),
                returnPrice
        );
    }

    private record RoutePlan(LocalDate outboundDate,
                             LocalTime outboundTime,
                             double outboundPrice,
                             LocalDate returnDate,
                             LocalTime returnTime,
                             double returnPrice) {
    }
}
