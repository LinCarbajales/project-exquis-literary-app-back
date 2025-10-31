package dev.lin.exquis.user;

import dev.lin.exquis.implementations.IUserService;
import dev.lin.exquis.user.dtos.UserRequestDTO;
import dev.lin.exquis.user.dtos.UserResponseDTO;

import java.util.List;

public interface UserService extends IUserService<UserResponseDTO, UserRequestDTO> {
    UserResponseDTO registerUser(UserRequestDTO userRequestDTO);

    @Override
    List<UserResponseDTO> getEntities();

    @Override
    UserResponseDTO getByID(Long id);

    @Override
    UserResponseDTO updateEntity(Long id, UserRequestDTO dto);

    @Override
    void deleteEntity(Long id);

    // Métodos para /users/me (por EMAIL, que es lo que devuelve Principal)
    UserResponseDTO getByEmail(String email);
    UserResponseDTO updateByEmail(String email, UserRequestDTO dto);
    void deleteByEmail(String email);
    
    // Métodos por username (si los necesitas en otro lugar)
    UserResponseDTO getByUsername(String username);
    UserResponseDTO updateByUsername(String username, UserRequestDTO dto);
    void deleteByUsername(String username);
}
