package ticket_train.ticketeer.repository;

import ticket_train.ticketeer.model.Ville;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VilleRepository extends JpaRepository<Ville, UUID> {
    Optional<Ville> findByNom(String nom);
}
