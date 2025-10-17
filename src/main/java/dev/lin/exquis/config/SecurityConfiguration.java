package dev.lin.exquis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.security.authentication.AuthenticationManager;
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
            .csrf(csrf -> csrf.disable())

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
                .requestMatchers("/error").permitAll()

                .requestMatchers(HttpMethod.POST, endpoint + "/users/register").permitAll()
                .requestMatchers(HttpMethod.POST, endpoint + "/login").permitAll()
                .requestMatchers(HttpMethod.POST, endpoint + "/logout").permitAll()
                
                // 🔒 ENDPOINTS PROTEGIDOS - Requieren autenticación
                .requestMatchers(endpoint + "/users/me/**").authenticated()
                .requestMatchers(HttpMethod.POST, endpoint + "/stories/**").authenticated()
                .requestMatchers(HttpMethod.POST, endpoint + "/collaborations/**").authenticated()
                .requestMatchers(endpoint + "/users/**").hasRole("ADMIN") // Solo admin puede ver todos los usuarios
                
                // Cualquier otro endpoint requiere autenticación
                .anyRequest().authenticated()
            )

            // --- Gestión de sesión: STATELESS ---
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // --- Servicio de usuarios ---
            .userDetailsService(jpaUserDetailsService)

            // --- Añadir el filtro JWT ---
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}