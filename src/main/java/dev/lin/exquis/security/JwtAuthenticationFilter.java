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
        

        final String authHeader = request.getHeader("Authorization");
        System.out.println("📨 Authorization Header: " + authHeader);
        

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ No hay token Bearer, continuando sin autenticación");
            filterChain.doFilter(request, response);
            return;
        }

        
        final String jwt = authHeader.substring(7);
        System.out.println("🎫 Token extraído (primeros 20 chars): " + jwt.substring(0, Math.min(20, jwt.length())) + "...");

        try {
            // 1️⃣ Extraer los datos del token
            final String userId = jwtService.extractUserId(jwt);
            final String userEmail = jwtService.extractEmail(jwt); // opcional, para logs

            System.out.println("🆔 ID extraído del token: " + userId);
            System.out.println("📧 Email (claim): " + userEmail);

            // 2️⃣ Si el contexto aún no tiene autenticación
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                System.out.println("🔓 Usuario no autenticado aún, procediendo a autenticar...");

                // 🔹 Nuevo método en tu JpaUserDetailsService (lo crearemos abajo)
                UserDetails userDetails = this.userDetailsService.loadUserById(Long.parseLong(userId));

                // 3️⃣ Validar el token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("🎉 Usuario autenticado correctamente (por ID)");
                } else {
                    System.out.println("❌ Token inválido o expirado");
                }
            }
        } catch (Exception e) {
            System.out.println("💥 ERROR procesando JWT: " + e.getMessage());
            e.printStackTrace();
        }


        filterChain.doFilter(request, response);
    }
}