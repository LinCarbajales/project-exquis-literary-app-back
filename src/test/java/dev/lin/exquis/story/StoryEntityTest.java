package dev.lin.exquis.story;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StoryEntity - Tests Unitarios")
class StoryEntityTest {

    @Test
    @DisplayName("Debe crear historia con valores por defecto usando @Builder.Default")
    void shouldCreateStoryWithDefaultValues() {
        // When
        StoryEntity story = StoryEntity.builder()
                .build();

        // Then
        assertThat(story).isNotNull();
        assertThat(story.getExtension()).isEqualTo(10); // Default value
        assertThat(story.isFinished()).isFalse(); // Default value
        assertThat(story.getCreatedAt()).isNotNull(); // Auto-set
    }

    @Test
    @DisplayName("Debe crear historia con builder y valores personalizados")
    void shouldCreateStoryWithCustomValues() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        StoryEntity story = StoryEntity.builder()
                .id(1L)
                .extension(15)
                .finished(true)
                .createdAt(now)
                .updatedAt(now.plusHours(1))
                .build();

        // Then
        assertThat(story.getId()).isEqualTo(1L);
        assertThat(story.getExtension()).isEqualTo(15);
        assertThat(story.isFinished()).isTrue();
        assertThat(story.getCreatedAt()).isEqualTo(now);
        assertThat(story.getUpdatedAt()).isEqualTo(now.plusHours(1));
    }

    @Test
    @DisplayName("Debe inicializar createdAt automáticamente")
    void shouldInitializeCreatedAtAutomatically() {
        // Given
        LocalDateTime before = LocalDateTime.now();

        // When
        StoryEntity story = StoryEntity.builder().build();

        // Then
        LocalDateTime after = LocalDateTime.now();
        assertThat(story.getCreatedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("Debe actualizar updatedAt en @PreUpdate")
    void shouldUpdateUpdatedAtOnPreUpdate() {
        // Given
        StoryEntity story = StoryEntity.builder()
                .extension(10)
                .finished(false)
                .build();

        LocalDateTime originalUpdatedAt = story.getUpdatedAt();

        // When
        story.onUpdate(); // Simular callback de JPA

        // Then
        assertThat(story.getUpdatedAt()).isNotNull();
        assertThat(story.getUpdatedAt()).isNotEqualTo(originalUpdatedAt);
    }

    @Test
    @DisplayName("Debe crear historia con constructor con todos los argumentos")
    void shouldCreateStoryWithAllArgsConstructor() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        StoryEntity story = new StoryEntity(
                1L,
                15,
                true,
                now,
                now.plusHours(1)
        );

        // Then
        assertThat(story.getId()).isEqualTo(1L);
        assertThat(story.getExtension()).isEqualTo(15);
        assertThat(story.isFinished()).isTrue();
        assertThat(story.getCreatedAt()).isEqualTo(now);
        assertThat(story.getUpdatedAt()).isEqualTo(now.plusHours(1));
    }

    @Test
    @DisplayName("Debe permitir modificar campos con setters")
    void shouldAllowModifyingFieldsWithSetters() {
        // Given
        StoryEntity story = new StoryEntity();
        LocalDateTime now = LocalDateTime.now();

        // When
        story.setId(1L);
        story.setExtension(20);
        story.setFinished(true);
        story.setCreatedAt(now);
        story.setUpdatedAt(now.plusHours(1));

        // Then
        assertThat(story.getId()).isEqualTo(1L);
        assertThat(story.getExtension()).isEqualTo(20);
        assertThat(story.isFinished()).isTrue();
        assertThat(story.getCreatedAt()).isEqualTo(now);
        assertThat(story.getUpdatedAt()).isEqualTo(now.plusHours(1));
    }

    @Test
    @DisplayName("Debe manejar extensión cero")
    void shouldHandleZeroExtension() {
        // When
        StoryEntity story = StoryEntity.builder()
                .extension(0)
                .build();

        // Then
        assertThat(story.getExtension()).isEqualTo(0);
    }

    @Test
    @DisplayName("Debe manejar extensión grande")
    void shouldHandleLargeExtension() {
        // When
        StoryEntity story = StoryEntity.builder()
                .extension(1000)
                .build();

        // Then
        assertThat(story.getExtension()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Debe cambiar estado finished de false a true")
    void shouldChangeFinishedStateFromFalseToTrue() {
        // Given
        StoryEntity story = StoryEntity.builder()
                .finished(false)
                .build();

        // When
        story.setFinished(true);

        // Then
        assertThat(story.isFinished()).isTrue();
    }

    @Test
    @DisplayName("Debe preservar ID al actualizar otros campos")
    void shouldPreserveIdWhenUpdatingOtherFields() {
        // Given
        StoryEntity story = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(false)
                .build();

        // When
        story.setExtension(20);
        story.setFinished(true);

        // Then
        assertThat(story.getId()).isEqualTo(1L); // ID no cambió
        assertThat(story.getExtension()).isEqualTo(20);
        assertThat(story.isFinished()).isTrue();
    }

    @Test
    @DisplayName("Debe implementar equals correctamente")
    void shouldImplementEqualsCorrectly() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        StoryEntity story1 = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(false)
                .createdAt(now)
                .build();

        StoryEntity story2 = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(false)
                .createdAt(now)
                .build();

        StoryEntity story3 = StoryEntity.builder()
                .id(2L)
                .extension(10)
                .finished(false)
                .createdAt(now)
                .build();

        // Then
        assertThat(story1).isEqualTo(story2);
        assertThat(story1).isNotEqualTo(story3);
    }

    @Test
    @DisplayName("Debe implementar hashCode correctamente")
    void shouldImplementHashCodeCorrectly() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        StoryEntity story1 = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(false)
                .createdAt(now)
                .build();

        StoryEntity story2 = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(false)
                .createdAt(now)
                .build();

        // Then
        assertThat(story1.hashCode()).isEqualTo(story2.hashCode());
    }

    @Test
    @DisplayName("Debe generar toString legible")
    void shouldGenerateReadableToString() {
        // Given
        StoryEntity story = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(false)
                .build();

        // When
        String toString = story.toString();

        // Then
        assertThat(toString).contains("StoryEntity");
        assertThat(toString).contains("id=1");
        assertThat(toString).contains("extension=10");
        assertThat(toString).contains("finished=false");
    }

    @Test
    @DisplayName("createdAt no debe cambiar después de actualización")
    void createdAtShouldNotChangeAfterUpdate() {
        // Given
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(1);
        StoryEntity story = StoryEntity.builder()
                .extension(10)
                .finished(false)
                .createdAt(originalCreatedAt)
                .build();

        // When
        story.setExtension(20);
        story.onUpdate();

        // Then
        assertThat(story.getCreatedAt()).isEqualTo(originalCreatedAt); // No cambió
        assertThat(story.getUpdatedAt()).isNotNull(); // Sí se actualizó
        assertThat(story.getUpdatedAt()).isAfter(originalCreatedAt);
    }

    @Test
    @DisplayName("Debe permitir null en updatedAt antes de actualización")
    void shouldAllowNullUpdatedAtBeforeUpdate() {
        // Given
        StoryEntity story = StoryEntity.builder()
                .extension(10)
                .finished(false)
                .build();

        // When - No se ha llamado a onUpdate()

        // Then
        assertThat(story.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("Debe manejar múltiples actualizaciones de updatedAt")
    void shouldHandleMultipleUpdatedAtUpdates() {
        // Given
        StoryEntity story = StoryEntity.builder()
                .extension(10)
                .finished(false)
                .build();

        // When
        story.onUpdate();
        LocalDateTime firstUpdate = story.getUpdatedAt();
        
        // Simular paso del tiempo
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        story.onUpdate();
        LocalDateTime secondUpdate = story.getUpdatedAt();

        // Then
        assertThat(firstUpdate).isNotNull();
        assertThat(secondUpdate).isNotNull();
        assertThat(secondUpdate).isAfterOrEqualTo(firstUpdate);
    }
}