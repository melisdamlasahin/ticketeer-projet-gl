package ticket_train.ticketeer.repository;

import ticket_train.ticketeer.model.Billet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BilletRepository extends JpaRepository<Billet, UUID> {
    Optional<Billet> findByCodeOptique(String codeOptique);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct b
            from Billet b
            left join fetch b.segments s
            left join fetch s.service
            where b.codeOptique = :codeOptique
            """)
    Optional<Billet> findForValidationByCodeOptique(@Param("codeOptique") String codeOptique);

    boolean existsByCodeOptique(String codeOptique);
}

