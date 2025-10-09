package dev.lin.exquis.auth.dtos;

/**
 * DTO de respuesta del login con JWT
 * Incluye el token JWT y los datos básicos del usuario
 */
public record AuthResponseDTO(
    String token,           // Token JWT
    UserDTO user           // Datos del usuario
) {
    
    /**
     * DTO interno para los datos del usuario
     */
    public record UserDTO(
        Long id_user,
        String username,
        String email
    ) {}
}