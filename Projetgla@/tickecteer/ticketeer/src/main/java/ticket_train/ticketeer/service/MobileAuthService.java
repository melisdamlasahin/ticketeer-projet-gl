package ticket_train.ticketeer.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ticket_train.ticketeer.dto.mobile.AuthResponse;
import ticket_train.ticketeer.dto.mobile.ClientProfileResponse;
import ticket_train.ticketeer.dto.mobile.LoginRequest;
import ticket_train.ticketeer.dto.mobile.RegisterRequest;
import ticket_train.ticketeer.dto.mobile.UpdateProfileRequest;
import ticket_train.ticketeer.model.Client;
import ticket_train.ticketeer.repository.ClientRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MobileAuthService {
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientTokenService clientTokenService;
    private final LoginAttemptService loginAttemptService;
    private final SecurityAuditService securityAuditService;

    public MobileAuthService(ClientRepository clientRepository,
                             PasswordEncoder passwordEncoder,
                             ClientTokenService clientTokenService,
                             LoginAttemptService loginAttemptService,
                             SecurityAuditService securityAuditService) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.clientTokenService = clientTokenService;
        this.loginAttemptService = loginAttemptService;
        this.securityAuditService = securityAuditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String sourceIp) {
        String email = normalized(request.getEmail());
        if (email == null || request.getMotDePasse() == null || request.getMotDePasse().isBlank()) {
            return authFailure("Informations d'inscription invalides");
        }
        if (clientRepository.findByEmail(email).isPresent()) {
            return authFailure("Un compte existe déjà avec cet email");
        }

        Client client = new Client(
                blankSafe(request.getNom(), "Utilisateur"),
                blankSafe(request.getPrenom(), ""),
                email,
                passwordEncoder.encode(request.getMotDePasse()),
                "photos/default-client.png"
        );
        clientRepository.save(client);
        securityAuditService.logRegistration(email, client.getClientId(), sourceIp);
        return buildAuthSuccess(client, "Inscription réussie");
    }

    public AuthResponse login(LoginRequest request, String sourceIp) {
        String email = normalized(request.getEmail());
        String rateLimitKey = loginAttemptKey(email, sourceIp);
        if (loginAttemptService.isBlocked(rateLimitKey)) {
            securityAuditService.logLoginRateLimited(email, sourceIp);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives de connexion. Réessayez plus tard.");
        }
        Optional<Client> clientOpt = email == null ? Optional.empty() : clientRepository.findByEmail(email);
        if (clientOpt.isEmpty()) {
            loginAttemptService.recordFailure(rateLimitKey);
            securityAuditService.logLoginFailure(email, sourceIp, "unknown_email");
            return authFailure("Identifiants invalides");
        }

        Client client = clientOpt.get();
        String hash = client.getHashMotDePasse();
        if (hash == null || request.getMotDePasse() == null || !passwordEncoder.matches(request.getMotDePasse(), hash)) {
            loginAttemptService.recordFailure(rateLimitKey);
            securityAuditService.logLoginFailure(email, sourceIp, "bad_password");
            return authFailure("Identifiants invalides");
        }

        loginAttemptService.recordSuccess(rateLimitKey);
        securityAuditService.logLoginSuccess(email, client.getClientId(), sourceIp);
        return buildAuthSuccess(client, "Connexion réussie");
    }

    public AuthResponse logout(String sourceIp) {
        UUID clientId = clientTokenService.requireAuthenticatedClientId();
        String token = clientTokenService.requireAuthenticatedToken();
        clientTokenService.revokeToken(token);
        securityAuditService.logLogout(clientId, sourceIp);
        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setMessage("Deconnexion reussie");
        return response;
    }

    public ClientProfileResponse getProfile(String token, String clientId) {
        return getProfile(clientId);
    }

    public ClientProfileResponse getProfile(String clientId) {
        return toProfileResponse(requireAuthorizedClient(clientId));
    }

    @Transactional
    public ClientProfileResponse updateProfile(String token, String clientId, UpdateProfileRequest request) {
        return updateProfile(clientId, request);
    }

    @Transactional
    public ClientProfileResponse updateProfile(String clientId, UpdateProfileRequest request) {
        Client client = requireAuthorizedClient(clientId);
        client.setNom(blankSafe(request.getNom(), client.getNom()));
        client.setPrenom(blankSafe(request.getPrenom(), client.getPrenom()));
        client.setSexe(blankToNull(request.getSexe()));
        client.setTelephone(blankToNull(request.getTelephone()));
        client.setDateNaissance(parseDate(blankToNull(request.getDateNaissance())));
        clientRepository.save(client);
        return toProfileResponse(client);
    }

    public Client requireAuthorizedClient(String token, String clientId) {
        return requireAuthorizedClient(clientId);
    }

    public Client requireAuthorizedClient(String clientId) {
        UUID requestedId = parseUuid(clientId, "Client invalide");
        UUID tokenClientId = clientTokenService.requireAuthenticatedClientId();
        if (!tokenClientId.equals(requestedId)) {
            securityAuditService.logForbiddenClientAccess(tokenClientId, clientId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return clientRepository.findById(requestedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable"));
    }

    private AuthResponse buildAuthSuccess(Client client, String message) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setClientId(client.getClientId().toString());
        response.setAuthToken(clientTokenService.issueToken(client.getClientId()));
        response.setNom(client.getNom());
        response.setPrenom(client.getPrenom());
        response.setEmail(client.getEmail());
        response.setSexe(client.getSexe());
        response.setDateNaissance(client.getDateNaissance() != null ? client.getDateNaissance().toString() : null);
        response.setTelephone(client.getTelephone());
        return response;
    }

    private AuthResponse authFailure(String message) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    private ClientProfileResponse toProfileResponse(Client client) {
        ClientProfileResponse response = new ClientProfileResponse();
        response.setClientId(client.getClientId().toString());
        response.setNom(client.getNom());
        response.setPrenom(client.getPrenom());
        response.setEmail(client.getEmail());
        response.setSexe(client.getSexe());
        response.setDateNaissance(client.getDateNaissance() != null ? client.getDateNaissance().toString() : null);
        response.setTelephone(client.getTelephone());
        return response;
    }

    private UUID parseUuid(String value, String message) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date invalide");
        }
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String blankSafe(String value, String fallback) {
        String normalizedValue = blankToNull(value);
        return normalizedValue != null ? normalizedValue : fallback;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String loginAttemptKey(String email, String sourceIp) {
        return (email != null ? email : "unknown") + "|" + (sourceIp != null ? sourceIp : "unknown");
    }
}
