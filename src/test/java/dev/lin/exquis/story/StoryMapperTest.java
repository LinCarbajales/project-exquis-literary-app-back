package dev.lin.exquis.story;

import dev.lin.exquis.story.dtos.StoryRequestDTO;
import dev.lin.exquis.story.dtos.StoryResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StoryMapper - Tests Unitarios")
class StoryMapperTest {

    private StoryMapper storyMapper;

    @BeforeEach
    void setUp() {
        storyMapper = new StoryMapper();
    }

    @Test
    @DisplayName("Debe convertir RequestDTO a Entity correctamente")
    void shouldConvertRequestDtoToEntity() {
        // Given
        StoryRequestDTO dto = StoryRequestDTO.builder()
                .extension(15)
                .finished(true)
                .build();

        // When
        StoryEntity entity = storyMapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getExtension()).isEqualTo(15);
        assertThat(entity.isFinished()).isTrue();
        assertThat(entity.getId()).isNull(); // No debe tener ID al crear
    }

    @Test
    @DisplayName("Debe retornar null cuando RequestDTO es null")
    void shouldReturnNullWhenRequestDtoIsNull() {
        // When
        StoryEntity entity = storyMapper.toEntity(null);

        // Then
        assertThat(entity).isNull();
    }

    @Test
    @DisplayName("Debe convertir Entity a ResponseDTO correctamente")
    void shouldConvertEntityToResponseDto() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        StoryEntity entity = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(false)
                .createdAt(now)
                .updatedAt(now.plusHours(1))
                .build();

        // When
        StoryResponseDTO dto = storyMapper.toResponseDTO(entity);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getExtension()).isEqualTo(10);
        assertThat(dto.isFinished()).isFalse();
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getUpdatedAt()).isEqualTo(now.plusHours(1));
    }

    @Test
    @DisplayName("Debe retornar null cuando Entity es null")
    void shouldReturnNullWhenEntityIsNull() {
        // When
        StoryResponseDTO dto = storyMapper.toResponseDTO(null);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("Debe actualizar Entity desde RequestDTO")
    void shouldUpdateEntityFromDto() {
        // Given
        StoryEntity entity = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(false)
                .build();

        StoryRequestDTO dto = StoryRequestDTO.builder()
                .extension(20)
                .finished(true)
                .build();

        // When
        storyMapper.updateEntityFromDTO(entity, dto);

        // Then
        assertThat(entity.getExtension()).isEqualTo(20);
        assertThat(entity.isFinished()).isTrue();
        assertThat(entity.getId()).isEqualTo(1L); // ID no debe cambiar
    }

    @Test
    @DisplayName("No debe actualizar extension si es null en DTO")
    void shouldNotUpdateExtensionIfNullInDto() {
        // Given
        StoryEntity entity = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(false)
                .build();

        StoryRequestDTO dto = StoryRequestDTO.builder()
                .extension(null)
                .finished(true)
                .build();

        // When
        storyMapper.updateEntityFromDTO(entity, dto);

        // Then
        assertThat(entity.getExtension()).isEqualTo(10); // No cambió
        assertThat(entity.isFinished()).isTrue(); // Sí cambió
    }

    @Test
    @DisplayName("No debe hacer nada si DTO es null")
    void shouldDoNothingIfDtoIsNull() {
        // Given
        StoryEntity entity = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(false)
                .build();

        // When
        storyMapper.updateEntityFromDTO(entity, null);

        // Then
        assertThat(entity.getExtension()).isEqualTo(10);
        assertThat(entity.isFinished()).isFalse();
    }

    @Test
    @DisplayName("No debe hacer nada si Entity es null")
    void shouldDoNothingIfEntityIsNull() {
        // Given
        StoryRequestDTO dto = StoryRequestDTO.builder()
                .extension(20)
                .finished(true)
                .build();

        // When & Then - No debe lanzar excepción
        assertThatCode(() -> storyMapper.updateEntityFromDTO(null, dto))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe manejar extension cero correctamente")
    void shouldHandleZeroExtensionCorrectly() {
        // Given
        StoryRequestDTO dto = StoryRequestDTO.builder()
                .extension(0)
                .finished(false)
                .build();

        // When
        StoryEntity entity = storyMapper.toEntity(dto);

        // Then
        assertThat(entity.getExtension()).isEqualTo(0);
    }

    @Test
    @DisplayName("Debe preservar campos no mapeados en actualización")
    void shouldPreserveUnmappedFieldsOnUpdate() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now();
        StoryEntity entity = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(false)
                .createdAt(createdAt)
                .build();

        StoryRequestDTO dto = StoryRequestDTO.builder()
                .extension(15)
                .finished(true)
                .build();

        // When
        storyMapper.updateEntityFromDTO(entity, dto);

        // Then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    }
}