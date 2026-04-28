package ticket_train.ticketeer.repository;

import ticket_train.ticketeer.model.Validation;
import ticket_train.ticketeer.model.Controleur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ValidationRepository extends JpaRepository<Validation, UUID> {
    List<Validation> findTop10ByControleurOrderByTimestampControleDesc(Controleur controleur);
    List<Validation> findTop5BySegmentOrderByTimestampControleDesc(ticket_train.ticketeer.model.SegmentBillet segment);
    List<Validation> findTop10BySegment_Billet_Client_ClientIdOrderByTimestampControleDesc(UUID clientId);
    long countByControleurAndTimestampControleAfter(Controleur controleur, LocalDateTime cutoff);
    long countByControleurAndResultatAndTimestampControleAfter(Controleur controleur,
                                                               ticket_train.ticketeer.model.enums.ValidationResult resultat,
                                                               LocalDateTime cutoff);
}
