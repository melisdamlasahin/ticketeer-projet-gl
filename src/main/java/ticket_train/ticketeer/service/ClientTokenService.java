package ticket_train.ticketeer.service;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ClientTokenService {

    private final ConcurrentMap<String, UUID> tokens = new ConcurrentHashMap<>();

    public String issueToken(UUID clientId) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, clientId);
        return token;
    }

    public Optional<UUID> resolveClientId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tokens.get(token));
    }
}
