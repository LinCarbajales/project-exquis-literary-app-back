package dev.lin.exquis.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lin.exquis.role.RoleEntity;
import dev.lin.exquis.role.RoleRepository;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController - Tests de Integración con Basic Auth")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${api-endpoint}")
    private String apiEndpoint;

    private RoleEntity userRole;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Limpiar en el orden correcto: primero usuarios (tienen FK a roles)
        userRepository.deleteAll();
        
        // Buscar o crear rol USER
        userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity();
                    newRole.setName("USER");
                    return roleRepository.save(newRole);
                });

        testUser = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .name("Test")
                .surname("User")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(userRole))
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("GET /login - Debe autenticar usuario con credenciales válidas usando Basic Auth")
    void shouldAuthenticateWithValidCredentials() throws Exception {
        // Given
        String credentials = "test@example.com:password123";
        String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

        // When & Then
        mockMvc.perform(get(apiEndpoint + "/login")
                        .header("Authorization", "Basic " + encodedCredentials))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.id_user").value(testUser.getId()))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("GET /login - Debe fallar con credenciales inválidas")
    void shouldFailWithInvalidCredentials() throws Exception {
        // Given
        String credentials = "test@example.com:wrongpassword";
        String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

        // When & Then
        mockMvc.perform(get(apiEndpoint + "/login")
                        .header("Authorization", "Basic " + encodedCredentials))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /login - Debe fallar con email inexistente")
    void shouldFailWithNonExistentEmail() throws Exception {
        // Given
        String credentials = "nonexistent@example.com:password123";
        String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

        // When & Then
        mockMvc.perform(get(apiEndpoint + "/login")
                        .header("Authorization", "Basic " + encodedCredentials))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /login - Debe fallar sin header Authorization")
    void shouldFailWithoutAuthorizationHeader() throws Exception {
        // When & Then
        mockMvc.perform(get(apiEndpoint + "/login"))
                .andExpect(status().isBadRequest()); // Spring devuelve 400 cuando falta header requerido
    }

    @Test
    @DisplayName("GET /login - Debe fallar con formato de Basic Auth incorrecto")
    void shouldFailWithInvalidBasicAuthFormat() throws Exception {
        // Given - Header sin "Basic " prefix
        String encodedCredentials = java.util.Base64.getEncoder().encodeToString("test@example.com:password123".getBytes());

        // When & Then
        mockMvc.perform(get(apiEndpoint + "/login")
                        .header("Authorization", encodedCredentials))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Se requiere autenticación Basic"));
    }

    @Test
    @DisplayName("GET /login - Debe fallar con credenciales mal formateadas")
    void shouldFailWithMalformedCredentials() throws Exception {
        // Given - Credenciales sin el formato email:password
        String credentials = "malformed-credentials-without-colon";
        String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

        // When & Then
        mockMvc.perform(get(apiEndpoint + "/login")
                        .header("Authorization", "Basic " + encodedCredentials))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /login - Token debe ser válido para autenticación")
    void tokenShouldBeValidForAuthentication() throws Exception {
        // Given
        String credentials = "test@example.com:password123";
        String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

        // When - Obtener token
        String response = mockMvc.perform(get(apiEndpoint + "/login")
                        .header("Authorization", "Basic " + encodedCredentials))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract token
        String token = objectMapper.readTree(response).get("token").asText();

        // Then - Usar token para acceder a endpoint protegido
        mockMvc.perform(get(apiEndpoint + "/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("GET /login - Debe manejar múltiples usuarios simultáneamente")
    void shouldHandleMultipleUsersSimultaneously() throws Exception {
        // Given
        UserEntity secondUser = UserEntity.builder()
                .username("seconduser")
                .email("second@example.com")
                .name("Second")
                .surname("User")
                .password(passwordEncoder.encode("password456"))
                .roles(Set.of(userRole))
                .build();
        userRepository.save(secondUser);

        String credentials1 = "test@example.com:password123";
        String credentials2 = "second@example.com:password456";
        String encodedCredentials1 = java.util.Base64.getEncoder().encodeToString(credentials1.getBytes());
        String encodedCredentials2 = java.util.Base64.getEncoder().encodeToString(credentials2.getBytes());

        // When & Then - Login usuario 1
        mockMvc.perform(get(apiEndpoint + "/login")
                        .header("Authorization", "Basic " + encodedCredentials1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("testuser"));

        // Login usuario 2
        mockMvc.perform(get(apiEndpoint + "/login")
                        .header("Authorization", "Basic " + encodedCredentials2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("seconduser"));
    }

    @Test
    @DisplayName("GET /login - Debe incluir todos los datos del usuario en respuesta")
    void shouldIncludeAllUserDataInResponse() throws Exception {
        // Given
        String credentials = "test@example.com:password123";
        String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

        // When & Then
        mockMvc.perform(get(apiEndpoint + "/login")
                        .header("Authorization", "Basic " + encodedCredentials))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.id_user").exists())
                .andExpect(jsonPath("$.user.username").exists())
                .andExpect(jsonPath("$.user.email").exists());
    }

    @Test
    @DisplayName("GET /logout - Debe retornar respuesta exitosa")
    void shouldLogoutSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(get(apiEndpoint + "/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout exitoso"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("GET /logout - Debe funcionar sin autenticación")
    void shouldLogoutWithoutAuthentication() throws Exception {
        // When & Then - El logout no requiere autenticación
        mockMvc.perform(get(apiEndpoint + "/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}