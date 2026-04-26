package ticket_train.ticketeer.repository;

import ticket_train.ticketeer.model.Controleur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ControleurRepository extends JpaRepository<Controleur, UUID> {
    Optional<Controleur> findByLogin(String login);
}
