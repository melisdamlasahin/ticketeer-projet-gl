package ticket_train.ticketeer.repository;

import ticket_train.ticketeer.model.Billet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BilletRepository extends JpaRepository<Billet, UUID> {
    Optional<Billet> findByCodeOptique(String codeOptique);
    boolean existsByCodeOptique(String codeOptique);
    List<Billet> findByClientClientId(UUID clientId);
}


