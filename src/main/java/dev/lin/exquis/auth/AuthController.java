package dev.lin.exquis.auth;

import dev.lin.exquis.auth.dtos.LoginRequest;
import dev.lin.exquis.security.JwtService;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("${api-endpoint}")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // 1️⃣ Autenticar usuario con Spring Security
try {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.email(),
            loginRequest.password()
        )
    );
} catch (Exception e) {
    System.out.println("❌ Error al autenticar: " + e.getClass().getSimpleName() + " - " + e.getMessage());
    throw e;
}

        // 2️⃣ Buscar el usuario en BD
        UserEntity user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + loginRequest.email()));

        // 3️⃣ Generar token con ID + email (nuevo JwtService)
        String jwt = jwtService.generateToken(user.getId(), user.getEmail());

        // 4️⃣ Construir respuesta
        Map<String, Object> response = Map.of(
                "token", jwt,
                "user", Map.of(
                        "id_user", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail()
                )
        );

        // 5️⃣ Devolver token y datos del usuario
        return ResponseEntity.ok(response);
    }
}