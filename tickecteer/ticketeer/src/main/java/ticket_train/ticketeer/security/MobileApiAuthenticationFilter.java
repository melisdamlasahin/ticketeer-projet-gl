package ticket_train.ticketeer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ticket_train.ticketeer.dto.ApiErrorResponse;
import ticket_train.ticketeer.service.ClientTokenService;
import ticket_train.ticketeer.service.SecurityAuditService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MobileApiAuthenticationFilter extends OncePerRequestFilter {

    private final ClientTokenService clientTokenService;
    private final SecurityAuditService securityAuditService;

    public MobileApiAuthenticationFilter(ClientTokenService clientTokenService,
                                         SecurityAuditService securityAuditService) {
        this.clientTokenService = clientTokenService;
        this.securityAuditService = securityAuditService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/services");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication existingAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuthentication != null && existingAuthentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (token == null) {
            securityAuditService.logMissingToken(request.getRequestURI(), request.getRemoteAddr());
            writeUnauthorized(response, "Auth token manquant");
            return;
        }

        if (clientTokenService.isRevoked(token)) {
            securityAuditService.logRevokedTokenUse(request.getRequestURI(), request.getRemoteAddr());
            writeUnauthorized(response, "Auth token invalide");
            return;
        }
        Optional<UUID> clientId = clientTokenService.resolveClientId(token);
        if (clientId.isEmpty()) {
            securityAuditService.logInvalidToken(request.getRequestURI(), request.getRemoteAddr());
            writeUnauthorized(response, "Auth token invalide");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                clientId.get().toString(),
                token,
                List.of(new SimpleGrantedAuthority("ROLE_MOBILE_CLIENT"))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            return authorization;
        }
        String legacyHeader = request.getHeader("X-Auth-Token");
        return legacyHeader != null && !legacyHeader.isBlank() ? legacyHeader : null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ApiErrorResponse body = new ApiErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                null
        );
        String json = "{\"status\":" + body.getStatus()
                + ",\"error\":\"" + body.getError()
                + "\",\"message\":\"" + body.getMessage()
                + "\",\"fieldErrors\":null}";
        response.getWriter().write(json);
    }
}
