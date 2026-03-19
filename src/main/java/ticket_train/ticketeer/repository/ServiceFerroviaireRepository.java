package ticket_train.ticketeer.repository;

import ticket_train.ticketeer.model.ServiceFerroviaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceFerroviaireRepository extends JpaRepository<ServiceFerroviaire, UUID> {
    List<ServiceFerroviaire> findByDateTrajet(LocalDate date);
}