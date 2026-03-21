package ticket_train.ticketeer.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ticket_train.ticketeer.model.Controleur;
import ticket_train.ticketeer.repository.ControleurRepository;

@Service
public class ControlUnitAuthService implements UserDetailsService {
    private final ControleurRepository controleurRepository;

    public ControlUnitAuthService(ControleurRepository controleurRepository) {
        this.controleurRepository = controleurRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Controleur controleur = controleurRepository.findByLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("Controller not found: " + username));

        return User.withUsername(controleur.getLogin())
                .password(controleur.getHashMotDePasse())
                .roles("CONTROLEUR")
                .build();
    }

    public Controleur getAuthenticatedControleur() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new UsernameNotFoundException("No authenticated controller");
        }

        return controleurRepository.findByLogin(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Controller not found: " + authentication.getName()));
    }
}
