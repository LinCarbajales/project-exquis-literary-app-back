package dev.lin.exquis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import dev.lin.exquis.security.JpaUserDetailsService;
import dev.lin.exquis.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${api-endpoint}")
    private String endpoint;

    private final JpaUserDetailsService jpaUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfiguration(
            JpaUserDetailsService jpaUserDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jpaUserDetailsService = jpaUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // --- CORS & CSRF ---
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable()) // Desactivar CSRF (JWT no lo necesita)

            // --- Headers ---
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            )

            // --- Desactivar formulario de login por defecto ---
            .formLogin(form -> form.disable())

            // --- Autorización de endpoints ---
            .authorizeHttpRequests(auth -> auth
                // Acceso público
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.POST, endpoint + "/users/register").permitAll()
                .requestMatchers(HttpMethod.POST, endpoint + "/login").permitAll() // Cambiado a POST
                .requestMatchers(HttpMethod.POST, endpoint + "/logout").permitAll() // Cambiado a POST

                // Cualquier otro endpoint requiere autenticación
                .anyRequest().authenticated()
            )

            // --- Gestión de sesión: STATELESS (sin sesiones, usamos JWT) ---
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // --- Proveedor de autenticación ---
            .authenticationProvider(authenticationProvider())

            // --- Añadir el filtro JWT antes del filtro de autenticación por defecto ---
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Proveedor de autenticación que usa nuestro UserDetailsService
     */
    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(jpaUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * AuthenticationManager para poder autenticar manualmente en el login
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}