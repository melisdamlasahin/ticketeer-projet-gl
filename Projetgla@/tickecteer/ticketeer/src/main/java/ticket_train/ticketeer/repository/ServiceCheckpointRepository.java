package ticket_train.ticketeer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ticket_train.ticketeer.model.ServiceCheckpoint;
import ticket_train.ticketeer.model.ServiceFerroviaire;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceCheckpointRepository extends JpaRepository<ServiceCheckpoint, UUID> {
    List<ServiceCheckpoint> findByServiceOrderByOrdreAsc(ServiceFerroviaire service);
    Optional<ServiceCheckpoint> findByServiceAndOrdre(ServiceFerroviaire service, Integer ordre);
}
