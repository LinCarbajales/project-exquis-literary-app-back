package dev.lin.exquis.user.dtos;

import java.util.Set;

public record UserResponseDTO(
    Long id_user,
    String username,
    String email,
    String name,
    String surname,
    Set<String> roles
) {
}