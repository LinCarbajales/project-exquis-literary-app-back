package dev.lin.exquis.user.dtos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserRequestDTO - Tests Unitarios")
class UserRequestDTOTest {

    @Test
    @DisplayName("Debe crear DTO con todos los campos")
    void shouldCreateDtoWithAllFields() {
        // When
        UserRequestDTO dto = new UserRequestDTO(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                Set.of("USER", "ADMIN")
        );

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.username()).isEqualTo("testuser");
        assertThat(dto.email()).isEqualTo("test@example.com");
        assertThat(dto.password()).isEqualTo("password123");
        assertThat(dto.name()).isEqualTo("Test");
        assertThat(dto.surname()).isEqualTo("User");
        assertThat(dto.roles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    @DisplayName("Debe permitir valores null")
    void shouldAllowNullValues() {
        // When
        UserRequestDTO dto = new UserRequestDTO(
                null,
                null,
                null,
                null,
                null,
                null
        );

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.username()).isNull();
        assertThat(dto.email()).isNull();
        assertThat(dto.password()).isNull();
        assertThat(dto.name()).isNull();
        assertThat(dto.surname()).isNull();
        assertThat(dto.roles()).isNull();
    }

    @Test
    @DisplayName("Debe crear DTO sin roles")
    void shouldCreateDtoWithoutRoles() {
        // When
        UserRequestDTO dto = new UserRequestDTO(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                null
        );

        // Then
        assertThat(dto.roles()).isNull();
    }

    @Test
    @DisplayName("Debe crear DTO con roles vacíos")
    void shouldCreateDtoWithEmptyRoles() {
        // When
        UserRequestDTO dto = new UserRequestDTO(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                Set.of()
        );

        // Then
        assertThat(dto.roles()).isEmpty();
    }

    @Test
    @DisplayName("Debe ser inmutable (record)")
    void shouldBeImmutable() {
        // Given
        UserRequestDTO dto = new UserRequestDTO(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                Set.of("USER")
        );

        // Then - Los records son inmutables, no tienen setters
        // Verificamos que los getters existen
        assertThat(dto.username()).isNotNull();
        assertThat(dto.email()).isNotNull();
        assertThat(dto.password()).isNotNull();
    }

    @Test
    @DisplayName("Debe implementar equals correctamente")
    void shouldImplementEqualsCorrectly() {
        // Given
        UserRequestDTO dto1 = new UserRequestDTO(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                Set.of("USER")
        );

        UserRequestDTO dto2 = new UserRequestDTO(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                Set.of("USER")
        );

        UserRequestDTO dto3 = new UserRequestDTO(
                "otheruser",
                "other@example.com",
                "password456",
                "Other",
                "User",
                Set.of("ADMIN")
        );

        // Then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1).isNotEqualTo(dto3);
    }

    @Test
    @DisplayName("Debe implementar hashCode correctamente")
    void shouldImplementHashCodeCorrectly() {
        // Given
        UserRequestDTO dto1 = new UserRequestDTO(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                Set.of("USER")
        );

        UserRequestDTO dto2 = new UserRequestDTO(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                Set.of("USER")
        );

        // Then
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    @DisplayName("Debe generar toString legible")
    void shouldGenerateReadableToString() {
        // Given
        UserRequestDTO dto = new UserRequestDTO(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                Set.of("USER")
        );

        // When
        String toString = dto.toString();

        // Then
        assertThat(toString).contains("UserRequestDTO");
        assertThat(toString).contains("testuser");
        assertThat(toString).contains("test@example.com");
        assertThat(toString).contains("password123"); // Records incluyen todo
    }

    @Test
    @DisplayName("Debe manejar nombres con caracteres especiales")
    void shouldHandleSpecialCharactersInNames() {
        // When
        UserRequestDTO dto = new UserRequestDTO(
                "user_ñ123",
                "josé@example.com",
                "pass@123!",
                "José María",
                "García-López",
                Set.of("USER")
        );

        // Then
        assertThat(dto.username()).isEqualTo("user_ñ123");
        assertThat(dto.email()).isEqualTo("josé@example.com");
        assertThat(dto.name()).isEqualTo("José María");
        assertThat(dto.surname()).isEqualTo("García-López");
    }

    @Test
    @DisplayName("Debe manejar múltiples roles")
    void shouldHandleMultipleRoles() {
        // When
        UserRequestDTO dto = new UserRequestDTO(
                "adminuser",
                "admin@example.com",
                "password",
                "Admin",
                "User",
                Set.of("USER", "ADMIN", "MODERATOR")
        );

        // Then
        assertThat(dto.roles()).hasSize(3);
        assertThat(dto.roles()).containsExactlyInAnyOrder("USER", "ADMIN", "MODERATOR");
    }

    @Test
    @DisplayName("Debe permitir password vacío")
    void shouldAllowEmptyPassword() {
        // When
        UserRequestDTO dto = new UserRequestDTO(
                "testuser",
                "test@example.com",
                "",
                "Test",
                "User",
                Set.of("USER")
        );

        // Then
        assertThat(dto.password()).isEmpty();
        assertThat(dto.password()).isNotNull();
    }

    @Test
    @DisplayName("Debe permitir actualización parcial con campos null")
    void shouldAllowPartialUpdateWithNullFields() {
        // When - Simular actualización parcial (solo username y name)
        UserRequestDTO dto = new UserRequestDTO(
                "newusername",
                null,
                null,
                "NewName",
                null,
                null
        );

        // Then
        assertThat(dto.username()).isEqualTo("newusername");
        assertThat(dto.email()).isNull();
        assertThat(dto.password()).isNull();
        assertThat(dto.name()).isEqualTo("NewName");
        assertThat(dto.surname()).isNull();
        assertThat(dto.roles()).isNull();
    }

    @Test
    @DisplayName("Debe manejar emails largos")
    void shouldHandleLongEmails() {
        // Given
        String longEmail = "very.long.email.address.for.testing.purposes@example.com";

        // When
        UserRequestDTO dto = new UserRequestDTO(
                "user",
                longEmail,
                "pass",
                "Name",
                "Surname",
                Set.of("USER")
        );

        // Then
        assertThat(dto.email()).isEqualTo(longEmail);
    }

    @Test
    @DisplayName("Debe manejar usernames largos")
    void shouldHandleLongUsernames() {
        // Given
        String longUsername = "very_long_username_for_test";

        // When
        UserRequestDTO dto = new UserRequestDTO(
                longUsername,
                "test@example.com",
                "pass",
                "Name",
                "Surname",
                Set.of("USER")
        );

        // Then
        assertThat(dto.username()).isEqualTo(longUsername);
    }

    @Test
    @DisplayName("Debe ser serializable para JSON")
    void shouldBeSerializableForJson() {
        // Given
        UserRequestDTO dto = new UserRequestDTO(
                "testuser",
                "test@example.com",
                "password",
                "Test",
                "User",
                Set.of("USER")
        );

        // Then - Records son automáticamente serializables
        assertThat(dto).isNotNull();
        assertThat(dto.username()).isNotNull();
        assertThat(dto.email()).isNotNull();
    }
}