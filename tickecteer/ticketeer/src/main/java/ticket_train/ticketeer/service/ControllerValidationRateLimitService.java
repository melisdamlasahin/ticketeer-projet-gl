package ticket_train.ticketeer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ControllerValidationRateLimitService {

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final Map<String, WindowState> states = new ConcurrentHashMap<>();

    @Autowired
    public ControllerValidationRateLimitService(
            @Value("${app.controller-validation-rate-limit-max-attempts:20}") int maxAttempts,
            @Value("${app.controller-validation-rate-limit-window-seconds:60}") long windowSeconds
    ) {
        this(maxAttempts, Duration.ofSeconds(windowSeconds), Clock.systemUTC());
    }

    public ControllerValidationRateLimitService(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    public void checkAllowed(String key) {
        Instant now = Instant.now(clock);
        WindowState state = states.compute(key, (ignored, current) -> refreshState(current, now));
        if (state.attempts >= maxAttempts) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de validations en peu de temps. Réessayez plus tard."
            );
        }
        state.attempts++;
        state.windowStartedAt = state.windowStartedAt == null ? now : state.windowStartedAt;
    }

    private WindowState refreshState(WindowState current, Instant now) {
        if (current == null || current.windowStartedAt == null) {
            return new WindowState();
        }
        if (current.windowStartedAt.plus(window).isAfter(now)) {
            return current;
        }
        return new WindowState();
    }

    private static final class WindowState {
        private int attempts;
        private Instant windowStartedAt;
    }
}
