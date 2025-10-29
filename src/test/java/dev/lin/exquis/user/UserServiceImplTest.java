package dev.lin.exquis.user;

import dev.lin.exquis.collaboration.CollaborationEntity;
import dev.lin.exquis.collaboration.CollaborationRepository;
import dev.lin.exquis.role.RoleEntity;
import dev.lin.exquis.role.RoleRepository;
import dev.lin.exquis.user.dtos.UserRequestDTO;
import dev.lin.exquis.user.dtos.UserResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - Tests Unitarios")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CollaborationRepository collaborationRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private RoleEntity userRole;
    private UserEntity testUser;
    private UserRequestDTO validUserRequest;

    @BeforeEach
    void setUp() {
        userRole = new RoleEntity();
        userRole.setId_role(1L);
        userRole.setName("USER");

        testUser = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .name("Test")
                .surname("User")
                .password("encodedPassword")
                .roles(Set.of(userRole))
                .build();

        validUserRequest = new UserRequestDTO(
                "newuser",
                "new@example.com",
                "password123",
                "New",
                "User",
                Set.of("USER")
        );
    }

    @Test
    @DisplayName("Debe registrar un nuevo usuario con rol USER por defecto")
    void shouldRegisterUserWithDefaultRole() {
        // Given
        when(userRepository.existsByEmail(validUserRequest.email())).thenReturn(false);
        when(userRepository.existsByUsername(validUserRequest.username())).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(validUserRequest.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // When
        UserResponseDTO result = userService.registerUser(validUserRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("test@example.com");
        verify(userRepository).save(any(UserEntity.class));
        verify(passwordEncoder).encode(validUserRequest.password());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el email ya existe")
    void shouldThrowExceptionWhenEmailExists() {
        // Given
        when(userRepository.existsByEmail(validUserRequest.email())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(validUserRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("email ya está registrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el username ya existe")
    void shouldThrowExceptionWhenUsernameExists() {
        // Given
        when(userRepository.existsByEmail(validUserRequest.email())).thenReturn(false);
        when(userRepository.existsByUsername(validUserRequest.username())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(validUserRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("seudónimo ya está en uso");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe obtener usuario por email")
    void shouldGetUserByEmail() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserResponseDTO result = userService.getByEmail("test@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.username()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Debe lanzar excepción si usuario por email no existe")
    void shouldThrowExceptionWhenUserByEmailNotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getByEmail("nonexistent@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado con email");
    }

    @Test
    @DisplayName("Debe actualizar usuario por email")
    void shouldUpdateUserByEmail() {
        // Given
        UserRequestDTO updateRequest = new UserRequestDTO(
                "updateduser",
                "test@example.com",
                null,
                "Updated",
                "Name",
                null
        );

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("updateduser")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // When
        UserResponseDTO result = userService.updateByEmail("test@example.com", updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Debe eliminar usuario y reasignar colaboraciones a NoUser")
    void shouldDeleteUserAndReassignCollaborationsToNoUser() {
        // Given
        UserEntity noUser = UserEntity.builder()
                .id(999L)
                .username("NoUser")
                .email("no-user@system.local")
                .name("Deleted")
                .surname("User")
                .password("placeholder")
                .roles(Set.of(userRole))
                .build();

        CollaborationEntity collab = CollaborationEntity.builder()
                .id(1L)
                .text("Test collaboration")
                .user(testUser)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("NoUser")).thenReturn(Optional.of(noUser));
        when(collaborationRepository.findAll()).thenReturn(List.of(collab));

        // When
        userService.deleteByEmail("test@example.com");

        // Then
        verify(collaborationRepository).saveAll(anyList());
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("No debe permitir eliminar al usuario NoUser")
    void shouldNotAllowDeletingNoUser() {
        // Given
        UserEntity noUser = UserEntity.builder()
                .id(999L)
                .username("NoUser")
                .email("no-user@system.local")
                .build();

        when(userRepository.findByEmail("no-user@system.local")).thenReturn(Optional.of(noUser));

        // When & Then
        assertThatThrownBy(() -> userService.deleteByEmail("no-user@system.local"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se puede eliminar el usuario del sistema 'NoUser'");

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe crear NoUser si no existe al eliminar un usuario")
    void shouldCreateNoUserIfNotExistsWhenDeletingUser() {
        // Given
        UserEntity noUser = UserEntity.builder()
                .id(999L)
                .username("NoUser")
                .email("no-user@system.local")
                .name("Deleted")
                .surname("User")
                .password("placeholder")
                .roles(Set.of(userRole))
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("NoUser")).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("placeholder")).thenReturn("encodedPlaceholder");
        when(userRepository.save(any(UserEntity.class))).thenReturn(noUser);
        when(collaborationRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        userService.deleteByEmail("test@example.com");

        // Then
        verify(userRepository).save(any(UserEntity.class)); // NoUser + delete
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("Debe obtener todos los usuarios")
    void shouldGetAllUsers() {
        // Given
        List<UserEntity> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserResponseDTO> result = userService.getEntities();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Debe obtener usuario por ID")
    void shouldGetUserById() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserResponseDTO result = userService.getByID(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id_user()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Debe actualizar contraseña cuando se proporciona")
    void shouldUpdatePasswordWhenProvided() {
        // Given
        UserRequestDTO updateRequest = new UserRequestDTO(
                null,
                null,
                "newPassword123",
                null,
                null,
                null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // When
        userService.updateEntity(1L, updateRequest);

        // Then
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("No debe actualizar contraseña si es null o vacía")
    void shouldNotUpdatePasswordWhenNullOrEmpty() {
        // Given
        UserRequestDTO updateRequest = new UserRequestDTO(
                "newusername",
                null,
                "",
                null,
                null,
                null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("newusername")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // When
        userService.updateEntity(1L, updateRequest);

        // Then
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Debe actualizar roles del usuario")
    void shouldUpdateUserRoles() {
        // Given
        RoleEntity adminRole = new RoleEntity();
        adminRole.setId_role(2L);
        adminRole.setName("ADMIN");

        UserRequestDTO updateRequest = new UserRequestDTO(
                null,
                null,
                null,
                null,
                null,
                Set.of("ADMIN")
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // When
        userService.updateEntity(1L, updateRequest);

        // Then
        verify(roleRepository).findByName("ADMIN");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el rol no existe")
    void shouldThrowExceptionWhenRoleNotFound() {
        // Given
        UserRequestDTO updateRequest = new UserRequestDTO(
                null,
                null,
                null,
                null,
                null,
                Set.of("NONEXISTENT_ROLE")
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("NONEXISTENT_ROLE")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateEntity(1L, updateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rol NONEXISTENT_ROLE no encontrado");
    }
}