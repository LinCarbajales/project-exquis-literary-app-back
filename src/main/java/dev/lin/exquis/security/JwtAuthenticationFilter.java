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
        
        System.out.println("\n🔍 === JWT FILTER DEBUG ===");
        System.out.println("📍 URL: " + request.getMethod() + " " + request.getRequestURI());
        
        // 1. Obtener el header Authorization
        final String authHeader = request.getHeader("Authorization");
        System.out.println("📨 Authorization Header: " + authHeader);
        
        // 2. Verificar que el header existe y empieza con "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ No hay token Bearer, continuando sin autenticación");
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraer el token (quitar "Bearer " del principio)
        final String jwt = authHeader.substring(7);
        System.out.println("🎫 Token extraído (primeros 20 chars): " + jwt.substring(0, Math.min(20, jwt.length())) + "...");
        
        final String userEmail;

        try {
            // 4. Extraer el email del token
            userEmail = jwtService.extractUsername(jwt);
            System.out.println("📧 Email extraído del token: " + userEmail);

            // 5. Verificar que el usuario no esté ya autenticado
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                System.out.println("🔓 Usuario no autenticado aún, procediendo a autenticar...");
                
                // 6. Cargar los detalles del usuario desde la base de datos
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                System.out.println("👤 Usuario cargado: " + userDetails.getUsername());
                System.out.println("🔐 Authorities: " + userDetails.getAuthorities());

                // 7. Validar el token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    System.out.println("✅ Token válido!");
                    
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
                    System.out.println("🎉 Usuario autenticado correctamente en SecurityContext");
                } else {
                    System.out.println("❌ Token inválido o expirado");
                }
            } else if (userEmail == null) {
                System.out.println("⚠️ No se pudo extraer el email del token");
            } else {
                System.out.println("ℹ️ Usuario ya autenticado en este request");
            }
        } catch (Exception e) {
            System.out.println("💥 ERROR procesando JWT: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("🔍 === FIN JWT FILTER DEBUG ===\n");
        
        // 11. Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}