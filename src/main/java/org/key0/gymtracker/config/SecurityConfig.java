package org.key0.gymtracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Wyłączamy na chwilę CSRF, żeby formularz rejestracji POST nas nie blokował na start
                .csrf(csrf -> csrf.disable())

                // 2. Reguły dostępu - OD NAJBARDZIEJ SZCZEGÓŁOWYCH DO OGÓLNYCH
                .authorizeHttpRequests(auth -> auth
                        // Wpuszczamy style, skrypty i obrazki
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // Wpuszczamy strony powitalne, błędy oraz logowanie/rejestrację
                        .requestMatchers("/", "/login", "/register", "/error").permitAll()

                        // DOPIERO NA SAMYM KOŃCU: wszystko inne wymaga zalogowania
                        .anyRequest().authenticated()
                )

                // 3. Konfiguracja formularza logowania
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/profile", true)
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}