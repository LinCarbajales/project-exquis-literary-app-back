package dev.lin.exquis.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lin.exquis.auth.dtos.LoginRequest;
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
import org.springframework.http.MediaType;
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
@DisplayName("AuthController - Tests de Integración")
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
    @DisplayName("POST /login - Debe autenticar usuario con credenciales válidas")
    void shouldAuthenticateWithValidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        // When & Then
        mockMvc.perform((apiEndpoint + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.id_user").value(testUser.getId()))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /login - Debe fallar con credenciales inválidas")
    void shouldFailWithInvalidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        // When & Then
        mockMvc.perform(post(apiEndpoint + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /login - Debe fallar con email inexistente")
    void shouldFailWithNonExistentEmail() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");

        // When & Then
        mockMvc.perform(post(apiEndpoint + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /login - Debe fallar con email vacío")
    void shouldFailWithEmptyEmail() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("", "password123");

        // When & Then
        mockMvc.perform(post(apiEndpoint + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /login - Debe fallar con contraseña vacía")
    void shouldFailWithEmptyPassword() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "");

        // When & Then
        mockMvc.perform(post(apiEndpoint + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /login - Token debe ser válido para autenticación")
    void tokenShouldBeValidForAuthentication() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        // When - Obtener token
        String response = mockMvc.perform(post(apiEndpoint + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract token (simplified - in real scenario parse JSON properly)
        String token = objectMapper.readTree(response).get("token").asText();

        // Then - Usar token para acceder a endpoint protegido
        mockMvc.perform(get(apiEndpoint + "/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /login - Debe generar tokens diferentes en logins sucesivos")
    void shouldGenerateDifferentTokensForSuccessiveLogins() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        // When - Primer login
        String response1 = mockMvc.perform(post(apiEndpoint + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token1 = objectMapper.readTree(response1).get("token").asText();

        // Segundo login
        String response2 = mockMvc.perform(post(apiEndpoint + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token2 = objectMapper.readTree(response2).get("token").asText();

        // Then - Los tokens deben ser diferentes
        assert(!token1.equals(token2));
    }

    @Test
    @DisplayName("POST /login - Debe manejar múltiples usuarios simultáneamente")
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

        LoginRequest request1 = new LoginRequest("test@example.com", "password123");
        LoginRequest request2 = new LoginRequest("second@example.com", "password456");

        // When & Then - Login usuario 1
        mockMvc.perform(post(apiEndpoint + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("testuser"));

        // Login usuario 2
        mockMvc.perform(post(apiEndpoint + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("seconduser"));
    }

    @Test
    @DisplayName("POST /login - Debe incluir todos los datos del usuario en respuesta")
    void shouldIncludeAllUserDataInResponse() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        // When & Then
        mockMvc.perform(post(apiEndpoint + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.id_user").exists())
                .andExpect(jsonPath("$.user.username").exists())
                .andExpect(jsonPath("$.user.email").exists());
    }
}