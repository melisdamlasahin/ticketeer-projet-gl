package ticket_train.ticketeer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/controleur/login", "/css/**", "/js/**", "/images/**").permitAll()
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
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
