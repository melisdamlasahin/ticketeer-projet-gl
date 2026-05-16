package ticket_train.ticketeer.repository;

import ticket_train.ticketeer.model.Billet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BilletRepository extends JpaRepository<Billet, UUID> {
    Optional<Billet> findByCodeOptique(String codeOptique);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Billet b where b.codeOptique = :codeOptique")
    Optional<Billet> findByCodeOptiqueForUpdate(String codeOptique);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Billet b where b.ticketId = :ticketId")
    Optional<Billet> findByTicketIdForUpdate(UUID ticketId);

    boolean existsByCodeOptique(String codeOptique);
    List<Billet> findByClientClientId(UUID clientId);
}


