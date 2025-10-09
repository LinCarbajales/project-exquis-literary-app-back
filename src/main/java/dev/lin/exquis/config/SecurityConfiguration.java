package dev.lin.exquis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import dev.lin.exquis.security.JpaUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${api-endpoint}")
    private String endpoint;

    private final JpaUserDetailsService jpaUserDetailsService;

    public SecurityConfiguration(JpaUserDetailsService jpaUserDetailsService) {
        this.jpaUserDetailsService = jpaUserDetailsService;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // --- CORS & CSRF ---
            .cors(withDefaults())
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
                .disable()
            )

            // --- Headers ---
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            )

            // --- Desactivar formulario de login por defecto ---
            .formLogin(form -> form.disable())

            // --- Autorización de endpoints ---
            .authorizeHttpRequests(auth -> auth
                // acceso total a H2 y endpoints públicos
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.POST, endpoint + "/users/register").permitAll()
                .requestMatchers(HttpMethod.GET, endpoint + "/login").permitAll()
                // logout también debe ser accesible
                .requestMatchers(HttpMethod.GET, endpoint + "/logout").permitAll()

                // cualquier otro endpoint requerirá autenticación
                .anyRequest().authenticated()
            )

            // --- Servicio de usuarios ---
            .userDetailsService(jpaUserDetailsService)

            // --- Autenticación básica (para pruebas o APIs sin JWT aún) ---
            .httpBasic(withDefaults())

            // --- Logout ---
            .logout(logout -> logout
                .logoutUrl(endpoint + "/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )

            // --- Gestión de sesión ---
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
