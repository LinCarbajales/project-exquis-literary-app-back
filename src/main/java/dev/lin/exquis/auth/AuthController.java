package dev.lin.exquis.auth;

import dev.lin.exquis.security.JwtService;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("${api-endpoint}")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public ResponseEntity<?> login(@RequestHeader("Authorization") String authHeader) {
        try {
            // 1️⃣ Validar que el header sea Basic Auth
            if (!authHeader.startsWith("Basic ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Se requiere autenticación Basic"));
            }

            // 2️⃣ Decodificar credenciales
            String base64Credentials = authHeader.substring(6);
            byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decodedBytes);
            
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Formato de credenciales inválido"));
            }
            
            String email = parts[0];
            String password = parts[1];

            // 3️⃣ Validar credenciales
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            
            if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
            }

            // 4️⃣ Buscar el usuario en BD
            UserEntity user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            // 5️⃣ Generar token JWT
            String jwt = jwtService.generateToken(user.getId(), user.getEmail());

            // 6️⃣ Construir respuesta
            Map<String, Object> response = Map.of(
                    "token", jwt,
                    "user", Map.of(
                            "id_user", user.getId(),
                            "username", user.getUsername(),
                            "email", user.getEmail()
                    )
            );

            return ResponseEntity.ok(response);
            
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Usuario no encontrado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al procesar login: " + e.getMessage()));
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout() {
        // El logout con JWT es principalmente del lado del cliente
        return ResponseEntity.ok(Map.of(
                "message", "Logout exitoso",
                "status", "success"
        ));
    }
}