package ticket_train.ticketeer.repository;

import ticket_train.ticketeer.model.Validation;
import ticket_train.ticketeer.model.enums.ValidationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ValidationRepository extends JpaRepository<Validation, UUID> {
    boolean existsBySegmentSegmentIdAndResultat(UUID segmentId, ValidationResult resultat);
}
