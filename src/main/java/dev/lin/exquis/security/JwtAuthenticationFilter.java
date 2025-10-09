package dev.lin.exquis.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que intercepta todas las peticiones HTTP
 * para validar el JWT en el header Authorization
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JpaUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, JpaUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 1. Obtener el header Authorization
        final String authHeader = request.getHeader("Authorization");
        
        // 2. Verificar que el header existe y empieza con "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Si no hay token, continuar con la cadena de filtros
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraer el token (quitar "Bearer " del principio)
        final String jwt = authHeader.substring(7);
        final String userEmail;

        try {
            // 4. Extraer el email del token
            userEmail = jwtService.extractUsername(jwt);

            // 5. Verificar que el usuario no esté ya autenticado
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // 6. Cargar los detalles del usuario desde la base de datos
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 7. Validar el token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    
                    // 8. Crear el objeto de autenticación
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    
                    // 9. Añadir detalles de la petición
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    // 10. Establecer la autenticación en el contexto de seguridad
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Si hay algún error al procesar el token, simplemente no autenticamos al usuario
            logger.error("Error procesando JWT: " + e.getMessage());
        }

        // 11. Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}