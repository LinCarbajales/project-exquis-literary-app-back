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

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

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
    
    // Helper para crear usuarios con Set mutable de roles
    private UserEntity createUser(String username, String email, RoleEntity... roles) {
        Set<RoleEntity> roleSet = new HashSet<>();
        for (RoleEntity role : roles) {
            roleSet.add(role);
        }
        
        return UserEntity.builder()
                .username(username)
                .email(email)
                .name("Test")
                .surname("User")
                .password("encoded")
                .roles(roleSet)
                .build();
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
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("GET /me - Debe obtener usuario autenticado")
    void shouldGetCurrentUser() throws Exception {
        // Given
        UserEntity user = createUser("testuser", "test@example.com", userRole);
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
        UserEntity user = createUser("oldusername", "test@example.com", userRole);
        user.setName("Old");
        user.setSurname("Name");
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
        UserEntity user = createUser("testuser", "test@example.com", userRole);
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
        UserEntity user1 = createUser("user1", "user1@example.com", userRole);
        user1.setName("User");
        user1.setSurname("One");

        UserEntity user2 = createUser("user2", "user2@example.com", userRole);
        user2.setName("User");
        user2.setSurname("Two");

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
        UserEntity user = createUser("testuser", "test@example.com", userRole);
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
        UserEntity user = createUser("oldusername", "old@example.com", userRole);
        user.setName("Old");
        user.setSurname("Name");
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
        UserEntity user = createUser("testuser", "test@example.com", userRole);
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
    @DisplayName("POST /register - Debe asignar roles personalizados si se proporcionan")
    void shouldAssignCustomRolesWhenProvided() throws Exception {
        // Given
        RoleEntity adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity();
                    newRole.setName("ADMIN");
                    return roleRepository.save(newRole);
                });

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