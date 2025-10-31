package dev.lin.exquis.auth;

import dev.lin.exquis.security.JwtService;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import dev.lin.exquis.verificationToken.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AuthController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService verificationTokenService;

    /**
     * LOGIN - Ahora verifica que el email esté verificado
     */
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

            // 3️⃣ Buscar el usuario en BD
            UserEntity user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            // 🔐 NUEVO: Verificar que el email esté verificado
            if (!user.isEmailVerified()) {
                log.warn("⚠️ Intento de login con email no verificado: {}", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Debes verificar tu email antes de iniciar sesión. Revisa tu bandeja de entrada.",
                    "emailNotVerified", true,
                    "email", user.getEmail()
                ));
            }

            // 4️⃣ Validar credenciales
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            
            if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
            }

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

            log.info("✅ Login exitoso para: {}", user.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Usuario no encontrado"));
        } catch (Exception e) {
            log.error("❌ Error en login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al procesar login: " + e.getMessage()));
        }
    }

    /**
     * VERIFICAR EMAIL
     */
    @GetMapping("/verify-email/{token}")
    public ResponseEntity<?> verifyEmail(@PathVariable String token) {
        log.info("🔍 Solicitud de verificación de email");
        
        try {
            UserEntity user = verificationTokenService.verifyToken(token);
            
            log.info("✅ Email verificado para: {}", user.getUsername());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "¡Email verificado correctamente! Ya puedes iniciar sesión."
            ));
            
        } catch (Exception e) {
            log.error("❌ Error al verificar email: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * REENVIAR EMAIL DE VERIFICACIÓN
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("📧 Solicitud de reenvío de verificación para: {}", email);
        
        try {
            UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            if (user.isEmailVerified()) {
                log.warn("⚠️ El email ya está verificado");
                throw new RuntimeException("El email ya está verificado");
            }
            
            verificationTokenService.createAndSendVerificationToken(user);
            
            log.info("✅ Email de verificación reenviado");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email de verificación reenviado. Revisa tu bandeja de entrada."
            ));
            
        } catch (Exception e) {
            log.error("❌ Error al reenviar verificación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of(
                "message", "Logout realizado con éxito",
                "status", "success"
        ));
    }
}
