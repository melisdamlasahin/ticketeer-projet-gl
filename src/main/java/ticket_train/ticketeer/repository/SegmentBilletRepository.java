package ticket_train.ticketeer.repository;

import ticket_train.ticketeer.model.SegmentBillet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SegmentBilletRepository extends JpaRepository<SegmentBillet, UUID> {
}