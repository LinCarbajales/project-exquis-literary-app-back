package dev.lin.exquis.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lin.exquis.role.RoleEntity;
import dev.lin.exquis.role.RoleRepository;
import dev.lin.exquis.user.dtos.UserRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("UserController - Tests de Integración")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Value("${api-endpoint}")
    private String apiEndpoint;

    private RoleEntity userRole;

    @BeforeEach
    void setUp() {
        // Limpiar usuarios primero (tienen FK a roles)
        userRepository.deleteAll();
        
        // Buscar o crear rol USER (no eliminar roles entre tests)
        userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity();
                    newRole.setName("USER");
                    return roleRepository.save(newRole);
                });
    }

    @Test
    @DisplayName("POST /register - Debe registrar un nuevo usuario")
    void shouldRegisterNewUser() throws Exception {
        // Given
        UserRequestDTO request = new UserRequestDTO(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                null
        );

        // When & Then
        mockMvc.perform(post(apiEndpoint + "/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test"))
                .andExpect(jsonPath("$.surname").value("User"))
                .andExpect(jsonPath("$.roles", hasItem("USER")));
    }

    @Test
    @DisplayName("POST /register - Debe fallar con email duplicado")
    void shouldFailWithDuplicateEmail() throws Exception {
        // Given
        UserRequestDTO firstRequest = new UserRequestDTO(
                "user1",
                "duplicate@example.com",
                "password123",
                "First",
                "User",
                null
        );

        mockMvc.perform(post(apiEndpoint + "/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk());

        UserRequestDTO duplicateRequest = new UserRequestDTO(
                "user2",
                "duplicate@example.com",
                "password456",
                "Second",
                "User",
                null
        );

        // When & Then
        mockMvc.perform(post(apiEndpoint + "/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("POST /register - Debe fallar con username duplicado")
    void shouldFailWithDuplicateUsername() throws Exception {
        // Given
        UserRequestDTO firstRequest = new UserRequestDTO(
                "sameusername",
                "email1@example.com",
                "password123",
                "First",
                "User",
                null
        );

        mockMvc.perform(post(apiEndpoint + "/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk());

        UserRequestDTO duplicateRequest = new UserRequestDTO(
                "sameusername",
                "email2@example.com",
                "password456",
                "Second",
                "User",
                null
        );

        // When & Then
        mockMvc.perform(post(apiEndpoint + "/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("GET /me - Debe obtener usuario autenticado")
    void shouldGetCurrentUser() throws Exception {
        // Given
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .name("Test")
                .surname("User")
                .password("encoded")
                .roles(Set.of(userRole))
                .build();
        userRepository.save(user);

        // When & Then
        mockMvc.perform(get(apiEndpoint + "/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("PUT /me - Debe actualizar usuario autenticado")
    void shouldUpdateCurrentUser() throws Exception {
        // Given
        UserEntity user = UserEntity.builder()
                .username("oldusername")
                .email("test@example.com")
                .name("Old")
                .surname("Name")
                .password("encoded")
                .roles(Set.of(userRole))
                .build();
        userRepository.save(user);

        UserRequestDTO updateRequest = new UserRequestDTO(
                "newusername",
                "test@example.com",
                null,
                "New",
                "Name",
                null
        );

        // When & Then
        mockMvc.perform(put(apiEndpoint + "/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newusername"))
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.surname").value("Name"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("DELETE /me - Debe eliminar usuario autenticado")
    void shouldDeleteCurrentUser() throws Exception {
        // Given
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .name("Test")
                .surname("User")
                .password("encoded")
                .roles(Set.of(userRole))
                .build();
        userRepository.save(user);

        // When & Then
        mockMvc.perform(delete(apiEndpoint + "/users/me"))
                .andExpect(status().isOk());

        // Verify user was deleted
        assert(userRepository.findByEmail("test@example.com").isEmpty());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("GET / - Debe obtener todos los usuarios")
    void shouldGetAllUsers() throws Exception {
        // Given
        UserEntity user1 = UserEntity.builder()
                .username("user1")
                .email("user1@example.com")
                .name("User")
                .surname("One")
                .password("encoded")
                .roles(Set.of(userRole))
                .build();

        UserEntity user2 = UserEntity.builder()
                .username("user2")
                .email("user2@example.com")
                .name("User")
                .surname("Two")
                .password("encoded")
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user1);
        userRepository.save(user2);

        // When & Then
        mockMvc.perform(get(apiEndpoint + "/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].username", containsInAnyOrder("user1", "user2")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("GET /{id} - Debe obtener usuario por ID")
    void shouldGetUserById() throws Exception {
        // Given
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .name("Test")
                .surname("User")
                .password("encoded")
                .roles(Set.of(userRole))
                .build();
        user = userRepository.save(user);

        // When & Then
        mockMvc.perform(get(apiEndpoint + "/users/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_user").value(user.getId()))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("PUT /{id} - Debe actualizar usuario por ID")
    void shouldUpdateUserById() throws Exception {
        // Given
        UserEntity user = UserEntity.builder()
                .username("oldusername")
                .email("old@example.com")
                .name("Old")
                .surname("Name")
                .password("encoded")
                .roles(Set.of(userRole))
                .build();
        user = userRepository.save(user);

        UserRequestDTO updateRequest = new UserRequestDTO(
                "newusername",
                "old@example.com",
                null,
                "New",
                "Name",
                null
        );

        // When & Then
        mockMvc.perform(put(apiEndpoint + "/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newusername"))
                .andExpect(jsonPath("$.name").value("New"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("DELETE /{id} - Debe eliminar usuario por ID")
    void shouldDeleteUserById() throws Exception {
        // Given
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .name("Test")
                .surname("User")
                .password("encoded")
                .roles(Set.of(userRole))
                .build();
        user = userRepository.save(user);
        Long userId = user.getId();

        // When & Then
        mockMvc.perform(delete(apiEndpoint + "/users/" + userId))
                .andExpect(status().isOk());

        // Verify deletion
        assert(userRepository.findById(userId).isEmpty());
    }

    @Test
    @DisplayName("GET /me - Debe fallar sin autenticación")
    void shouldFailGetCurrentUserWithoutAuth() throws Exception {
        mockMvc.perform(get(apiEndpoint + "/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("GET /{id} - Debe fallar con ID inexistente")
    void shouldFailGetUserByNonExistentId() throws Exception {
        mockMvc.perform(get(apiEndpoint + "/users/9999"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("POST /register - Debe asignar roles personalizados si se proporcionan")
    void shouldAssignCustomRolesWhenProvided() throws Exception {
        // Given
        RoleEntity adminRole = new RoleEntity();
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);

        UserRequestDTO request = new UserRequestDTO(
                "adminuser",
                "admin@example.com",
                "password123",
                "Admin",
                "User",
                Set.of("ADMIN", "USER")
        );

        // When & Then
        mockMvc.perform(post(apiEndpoint + "/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasSize(2)))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("ADMIN", "USER")));
    }
}