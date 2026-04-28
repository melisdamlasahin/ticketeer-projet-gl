package ticket_train.ticketeer.config;

import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ticket_train.ticketeer.security.MobileApiAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final MobileApiAuthenticationFilter mobileApiAuthenticationFilter;

    public SecurityConfig(MobileApiAuthenticationFilter mobileApiAuthenticationFilter) {
        this.mobileApiAuthenticationFilter = mobileApiAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/controleur/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/services").permitAll()
                .requestMatchers("/api/**").hasRole("MOBILE_CLIENT")
                .requestMatchers("/controleur/**").hasRole("CONTROLEUR")
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/controleur/login")
                .loginProcessingUrl("/controleur/login")
                .defaultSuccessUrl("/controleur/home", true)
                .failureUrl("/controleur/login?error=true")
                .usernameParameter("login")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/controleur/logout")
                .logoutSuccessUrl("/controleur/login?logout=true")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/controleur/api/**")
            )
            .addFilterBefore(mobileApiAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
