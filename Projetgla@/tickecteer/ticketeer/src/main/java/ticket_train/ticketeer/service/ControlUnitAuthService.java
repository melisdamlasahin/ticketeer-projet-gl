package ticket_train.ticketeer.service;

import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.repository.ControleurRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class ControlUnitAuthService implements UserDetailsService {

    private final ControleurRepository controleurRepository;

    public ControlUnitAuthService(ControleurRepository controleurRepository) {
        this.controleurRepository = controleurRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Controleur controleur = controleurRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Controleur non trouve: " + login));

        return new User(
                controleur.getLogin(),
                controleur.getHashMotDePasse(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CONTROLEUR"))
        );
    }

    public Optional<Controleur> findByLogin(String login) {
        return controleurRepository.findByLogin(login);
    }
}
