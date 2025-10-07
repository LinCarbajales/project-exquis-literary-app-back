package dev.lin.exquis.user.dtos;

import java.util.Set;

public record UserRequestDTO(
    String username,
    String email,
    String password,
    String name,
    String surname,
    Set<String> roles
) {
}