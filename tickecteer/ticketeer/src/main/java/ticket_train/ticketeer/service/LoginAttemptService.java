package ticket_train.ticketeer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final int maxFailures;
    private final Duration blockDuration;
    private final Clock clock;
    private final Map<String, AttemptState> states = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(@Value("${app.mobile-login-rate-limit-max-failures:5}") int maxFailures,
                               @Value("${app.mobile-login-rate-limit-block-minutes:15}") long blockMinutes) {
        this(maxFailures, Duration.ofMinutes(blockMinutes), Clock.systemUTC());
    }

    LoginAttemptService(int maxFailures, Duration blockDuration, Clock clock) {
        this.maxFailures = maxFailures;
        this.blockDuration = blockDuration;
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        AttemptState state = states.get(key);
        if (state == null) {
            return false;
        }
        Instant now = Instant.now(clock);
        if (state.blockedUntil != null && state.blockedUntil.isAfter(now)) {
            return true;
        }
        if (state.blockedUntil != null && !state.blockedUntil.isAfter(now)) {
            states.remove(key);
        }
        return false;
    }

    public void recordFailure(String key) {
        states.compute(key, (ignored, current) -> {
            Instant now = Instant.now(clock);
            AttemptState state = current;
            if (state == null || state.blockedUntil != null && !state.blockedUntil.isAfter(now)) {
                state = new AttemptState();
            }
            state.failures++;
            state.lastFailureAt = now;
            if (state.failures >= maxFailures) {
                state.blockedUntil = now.plus(blockDuration);
            }
            return state;
        });
    }

    public void recordSuccess(String key) {
        states.remove(key);
    }

    private static final class AttemptState {
        private int failures;
        private Instant lastFailureAt;
        private Instant blockedUntil;
    }
}
