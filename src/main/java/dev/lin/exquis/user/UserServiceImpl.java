package dev.lin.exquis.user;

import dev.lin.exquis.collaboration.CollaborationEntity;
import dev.lin.exquis.collaboration.CollaborationRepository;
import dev.lin.exquis.role.RoleEntity;
import dev.lin.exquis.role.RoleRepository;
import dev.lin.exquis.user.dtos.UserRequestDTO;
import dev.lin.exquis.user.dtos.UserResponseDTO;
import dev.lin.exquis.verificationToken.VerificationTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importación necesaria

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private VerificationTokenService verificationTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CollaborationRepository collaborationRepository;

    @Override
    public UserResponseDTO registerUser(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new RuntimeException("El email ya está registrado");
        }

        if (userRepository.existsByUsername(dto.username())) {
            throw new RuntimeException("El seudónimo ya está en uso");
        }

        log.info("📝 Registrando nuevo usuario: {}", dto.username());

        UserEntity user = UserEntity.builder()
                .username(dto.username())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .name(dto.name())
                .surname(dto.surname())
                .emailVerified(false) // 🔐 Usuario NO verificado al registrarse
                .build();

        Set<RoleEntity> userRoles = new HashSet<>();
        if (dto.roles() == null || dto.roles().isEmpty()) {
            RoleEntity defaultRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Rol USER no encontrado"));
            userRoles.add(defaultRole);
        } else {
            for (String roleName : dto.roles()) {
                RoleEntity role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Rol " + roleName + " no encontrado"));
                userRoles.add(role);
            }
        }
        user.setRoles(userRoles);

        UserEntity savedUser = userRepository.save(user);
        log.info("✅ Usuario {} registrado con ID {}", savedUser.getUsername(), savedUser.getId());
        
        // 📧 ENVIAR EMAIL DE VERIFICACIÓN
        try {
            verificationTokenService.createAndSendVerificationToken(savedUser);
            log.info("📧 Email de verificación enviado a: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("❌ Error al enviar email de verificación: {}", e.getMessage());
            // No lanzamos excepción para que el registro se complete igual
        }
        
        return mapToResponseDTO(savedUser);
    }

    @Override
    public List<UserResponseDTO> getEntities() {
        return userRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO getByID(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return mapToResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateEntity(Long id, UserRequestDTO dto) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        updateUserFields(user, dto);

        UserEntity updated = userRepository.save(user);
        log.info("Usuario {} actualizado", updated.getUsername());
        return mapToResponseDTO(updated);
    }

    // 👑 MÉTODO DE ELIMINACIÓN POR ID (ADMIN PANEL) - AHORA REASIGNA COLABORACIONES
    @Override
    @Transactional // Para asegurar que la reasignación y eliminación sean atómicas
    public void deleteEntity(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El usuario no existe"));
        
        deleteAndReassignCollaborations(user);
        log.warn("Usuario con ID {} eliminado por Admin (colaboraciones reasignadas a NoUser)", id);
    }

    /* ==========================================================
       🔹 MÉTODOS PARA /users/me (por EMAIL)
       ========================================================== */

    @Override
    public UserResponseDTO getByEmail(String email) {
        log.info("Buscando usuario por email: {}", email);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
        return mapToResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateByEmail(String email, UserRequestDTO dto) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));

        updateUserFields(user, dto);

        UserEntity updated = userRepository.save(user);
        log.info("Usuario {} actualizado mediante /me", updated.getUsername());
        return mapToResponseDTO(updated);
    }

    // 🗑️ MÉTODO DE ELIMINACIÓN POR EMAIL (SELF-DELETE) - AHORA USA EL MÉTODO AUXILIAR
    @Override
    @Transactional // Para asegurar que la reasignación y eliminación sean atómicas
    public void deleteByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));

        deleteAndReassignCollaborations(user);
        log.warn("Usuario {} eliminado (colaboraciones reasignadas a NoUser)", email);
    }


    /* ==========================================================
       🔹 MÉTODOS POR USERNAME (heredados de la interfaz)
       ========================================================== */

    @Override
    public UserResponseDTO getByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return mapToResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateByUsername(String username, UserRequestDTO dto) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        updateUserFields(user, dto);

        UserEntity updated = userRepository.save(user);
        log.info("Usuario {} actualizado", updated.getUsername());
        return mapToResponseDTO(updated);
    }

    @Override
    public void deleteByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        userRepository.delete(user);
        log.warn("Usuario {} eliminado", username);
    }

    /* ==========================================================
       🔸 MÉTODOS AUXILIARES
       ========================================================== */

    /**
     * Lógica compartida para reasignar todas las colaboraciones de un usuario 
     * al usuario de sistema 'NoUser' antes de eliminar la cuenta.
     * @param userToDelete La entidad de usuario a eliminar.
     */
    private void deleteAndReassignCollaborations(UserEntity userToDelete) {
        // No permitir borrar al propio NoUser
        if ("NoUser".equals(userToDelete.getUsername())) {
            throw new RuntimeException("No se puede eliminar el usuario del sistema 'NoUser'");
        }

        // 🔹 Obtener o crear NoUser
        UserEntity noUser = getOrCreateNoUser();

        // 🔹 Buscar colaboraciones del usuario (reutilizando el patrón existente)
        List<CollaborationEntity> collaborations = collaborationRepository.findAll().stream()
                .filter(c -> c.getUser().getId().equals(userToDelete.getId()))
                .toList();

        if (!collaborations.isEmpty()) {
            collaborations.forEach(c -> c.setUser(noUser));
            collaborationRepository.saveAll(collaborations);
            log.info("Reasignadas {} colaboraciones del usuario {} a NoUser", collaborations.size(), userToDelete.getEmail());
        }

        // 🔹 Eliminar al usuario
        userRepository.delete(userToDelete);
    }


    private void updateUserFields(UserEntity user, UserRequestDTO dto) {
        if (dto.username() != null && !dto.username().isBlank()) {
            if (!user.getUsername().equals(dto.username()) && userRepository.existsByUsername(dto.username())) {
                throw new RuntimeException("El seudónimo ya está en uso");
            }
            user.setUsername(dto.username());
        }
        if (dto.email() != null && !dto.email().isBlank()) {
            if (!user.getEmail().equals(dto.email()) && userRepository.existsByEmail(dto.email())) {
                throw new RuntimeException("El email ya está en uso");
            }
            user.setEmail(dto.email());
        }
        if (dto.name() != null) user.setName(dto.name());
        if (dto.surname() != null) user.setSurname(dto.surname());
        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }

        if (dto.roles() != null && !dto.roles().isEmpty()) {
            Set<RoleEntity> newRoles = dto.roles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new RuntimeException("Rol " + roleName + " no encontrado")))
                    .collect(Collectors.toSet());
            user.setRoles(newRoles);
        }
    }

    private UserResponseDTO mapToResponseDTO(UserEntity user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());

        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getSurname(),
                roleNames
        );
    }


    // Método para crear NoUser y reasignarle colaboraciones al eliminar una cuenta

    private UserEntity getOrCreateNoUser() {
    return userRepository.findByUsername("NoUser")
        .orElseGet(() -> {
            UserEntity noUser = UserEntity.builder()
                    .username("NoUser")
                    .email("no-user@system.local")
                    .name("Deleted")
                    .surname("User")
                    .password(passwordEncoder.encode("placeholder")) // nunca se usará
                    .roles(Set.of(roleRepository.findByName("USER")
                            .orElseThrow(() -> new RuntimeException("Rol USER no encontrado"))))
                    .build();
            return userRepository.save(noUser);
        });
    }
}