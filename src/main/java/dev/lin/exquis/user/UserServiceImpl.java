package dev.lin.exquis.user;

import dev.lin.exquis.role.RoleEntity;
import dev.lin.exquis.role.RoleEntity.RoleName;
import dev.lin.exquis.role.RoleRepository;
import dev.lin.exquis.user.dtos.UserRequestDTO;
import dev.lin.exquis.user.dtos.UserResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDTO registerUser(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new RuntimeException("El email ya está registrado");
        }

        if (userRepository.existsByUsername(dto.username())) {
            throw new RuntimeException("El seudónimo ya está en uso");
        }

        log.info("Registrando nuevo usuario: {}", dto.username());

        UserEntity user = UserEntity.builder()
                .username(dto.username())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .name(dto.name())
                .surname(dto.surname())
                .build();

        Set<RoleEntity> userRoles = new HashSet<>();
        if (dto.roles() == null || dto.roles().isEmpty()) {
            RoleEntity defaultRole = roleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Rol USER no encontrado"));
            userRoles.add(defaultRole);
        } else {
            for (String roleName : dto.roles()) {
                RoleEntity role = roleRepository.findByName(RoleName.valueOf(roleName))
                        .orElseThrow(() -> new RuntimeException("Rol " + roleName + " no encontrado"));
                userRoles.add(role);
            }
        }
        user.setRoles(userRoles);

        UserEntity savedUser = userRepository.save(user);
        log.info("Usuario {} registrado con ID {}", savedUser.getUsername(), savedUser.getId());

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

        if (dto.username() != null) user.setUsername(dto.username());
        if (dto.email() != null) user.setEmail(dto.email());
        if (dto.name() != null) user.setName(dto.name());
        if (dto.surname() != null) user.setSurname(dto.surname());
        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }

        if (dto.roles() != null && !dto.roles().isEmpty()) {
            Set<RoleEntity> newRoles = dto.roles().stream()
                    .map(r -> roleRepository.findByName(RoleName.valueOf(r))
                            .orElseThrow(() -> new RuntimeException("Rol " + r + " no encontrado")))
                    .collect(Collectors.toSet());
            user.setRoles(newRoles);
        }

        UserEntity updated = userRepository.save(user);
        log.info("Usuario {} actualizado", updated.getUsername());
        return mapToResponseDTO(updated);
    }

    @Override
    public void deleteEntity(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("El usuario no existe");
        }
        userRepository.deleteById(id);
        log.warn("Usuario con ID {} eliminado", id);
    }

    private UserResponseDTO mapToResponseDTO(UserEntity user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
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
}
