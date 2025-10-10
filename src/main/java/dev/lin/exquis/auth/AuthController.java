package dev.lin.exquis.auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lin.exquis.security.JwtService;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import dev.lin.exquis.auth.dtos.LoginRequest;
import dev.lin.exquis.auth.dtos.AuthResponseDTO;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Controlador de autenticación con JWT
 */
@RestController
@RequestMapping(path = "${api-endpoint}")
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Endpoint de login - ahora con POST y JWT
     * Recibe email y password, devuelve token JWT y datos del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        System.out.println("🔵 ===== INICIO LOGIN =====");
        System.out.println("🔵 Email recibido: " + loginRequest.email());
        
        try {
            System.out.println("🔵 Intentando autenticar...");
            
            // 1. Autenticar al usuario con Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.email(),
                    loginRequest.password()
                )
            );

            System.out.println("✅ Autenticación exitosa");

            // 2. Si llega aquí, las credenciales son correctas
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            System.out.println("🔵 Generando token JWT...");
            
            // 3. Generar el token JWT
            String jwtToken = jwtService.generateToken(userDetails);
            
            System.out.println("✅ Token generado: " + jwtToken.substring(0, 20) + "...");
            
            // 4. Obtener los datos completos del usuario
            UserEntity user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            System.out.println("✅ Usuario encontrado: " + user.getUsername());
            
            // 5. Crear el DTO de respuesta con el token y los datos del usuario
            AuthResponseDTO.UserDTO userDTO = new AuthResponseDTO.UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail()
            );
            
            AuthResponseDTO response = new AuthResponseDTO(jwtToken, userDTO);
            
            System.out.println("✅ ===== LOGIN EXITOSO =====");
            return ResponseEntity.ok(response);
            
        } catch (BadCredentialsException e) {
            // Credenciales incorrectas
            System.err.println("❌ LOGIN FALLIDO - Credenciales incorrectas para: " + loginRequest.email());
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Credenciales incorrectas");
                
        } catch (Exception e) {
            // Otro error
            System.err.println("❌ LOGIN FALLIDO - Error inesperado:");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensaje: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error en el servidor: " + e.getMessage());
        }
    }

    /**
     * Endpoint de logout (opcional, ya que JWT es stateless)
     * En JWT el "logout" se hace eliminando el token del cliente
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // Con JWT no necesitamos hacer nada en el servidor
        // El cliente simplemente elimina el token
        System.out.println("🔵 Logout solicitado");
        return ResponseEntity.ok("Logout exitoso");
    }
}