package ticket_train.ticketeer.repository;

import ticket_train.ticketeer.model.Validation;
import ticket_train.ticketeer.model.Controleur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ValidationRepository extends JpaRepository<Validation, UUID> {
    List<Validation> findTop10ByControleurOrderByTimestampControleDesc(Controleur controleur);
}
