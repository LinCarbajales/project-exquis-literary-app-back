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
}
