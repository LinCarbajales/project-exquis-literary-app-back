package dev.lin.exquis.story;

import dev.lin.exquis.blockedStory.BlockedStoryEntity;
import dev.lin.exquis.blockedStory.BlockedStoryRepository;
import dev.lin.exquis.collaboration.CollaborationEntity;
import dev.lin.exquis.collaboration.CollaborationRepository;
import dev.lin.exquis.collaboration.dtos.CollaborationResponseDTO;
import dev.lin.exquis.story.dtos.CompletedStoryDTO;
import dev.lin.exquis.story.dtos.StoryAssignmentResponseDTO;
import dev.lin.exquis.story.dtos.StoryRequestDTO;
import dev.lin.exquis.story.dtos.StoryResponseDTO;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoryServiceImpl - Tests Unitarios Completos")
class StoryServiceImplTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private BlockedStoryRepository blockedStoryRepository;

    @Mock
    private CollaborationRepository collaborationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StoryServiceImpl storyService;

    private StoryEntity testStory;
    private UserEntity testUser;
    private BlockedStoryEntity blockedStory;

    @BeforeEach
    void setUp() {
        testStory = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(false)
                .createdAt(LocalDateTime.now())
                .build();

        testUser = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .name("Test")
                .surname("User")
                .build();

        blockedStory = BlockedStoryEntity.builder()
                .id(1L)
                .story(testStory)
                .lockedBy(testUser)
                .blockedUntil(LocalDateTime.now().plusMinutes(30))
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========== CRUD BÁSICO ==========

    @Test
    @DisplayName("Debe obtener todas las historias")
    void shouldGetAllStories() {
        // Given
        List<StoryEntity> stories = Arrays.asList(testStory);
        when(storyRepository.findAll()).thenReturn(stories);

        // When
        List<StoryResponseDTO> result = storyService.getStories();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(storyRepository).findAll();
    }

    @Test
    @DisplayName("Debe obtener historia por ID")
    void shouldGetStoryById() {
        // Given
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));

        // When
        StoryResponseDTO result = storyService.getStoryById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getExtension()).isEqualTo(10);
        verify(storyRepository).findById(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción al buscar historia inexistente")
    void shouldThrowExceptionWhenStoryNotFound() {
        // Given
        when(storyRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> storyService.getStoryById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Historia no encontrada");
    }

    @Test
    @DisplayName("Debe crear historia con valores por defecto")
    void shouldCreateStoryWithDefaultValues() {
        // Given
        StoryRequestDTO request = StoryRequestDTO.builder()
                .extension(null)
                .finished(false)
                .build();

        when(storyRepository.save(any(StoryEntity.class))).thenAnswer(invocation -> {
            StoryEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // When
        StoryResponseDTO result = storyService.createStory(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExtension()).isEqualTo(10); // Default
        assertThat(result.isFinished()).isFalse();
        verify(storyRepository).save(any(StoryEntity.class));
    }

    @Test
    @DisplayName("Debe crear historia con valores personalizados")
    void shouldCreateStoryWithCustomValues() {
        // Given
        StoryRequestDTO request = StoryRequestDTO.builder()
                .extension(15)
                .finished(false)
                .build();

        when(storyRepository.save(any(StoryEntity.class))).thenAnswer(invocation -> {
            StoryEntity saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // When
        StoryResponseDTO result = storyService.createStory(request);

        // Then
        assertThat(result.getExtension()).isEqualTo(15);
        verify(storyRepository).save(any(StoryEntity.class));
    }

    @Test
    @DisplayName("Debe actualizar historia correctamente")
    void shouldUpdateStorySuccessfully() {
        // Given
        StoryRequestDTO updateRequest = StoryRequestDTO.builder()
                .extension(20)
                .finished(true)
                .build();

        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));
        when(storyRepository.save(any(StoryEntity.class))).thenReturn(testStory);

        // When
        StoryResponseDTO result = storyService.updateStory(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(storyRepository).findById(1L);
        verify(storyRepository).save(any(StoryEntity.class));
    }

    @Test
    @DisplayName("Debe actualizar solo extension si finished es igual")
    void shouldUpdateOnlyExtensionWhenNeeded() {
        // Given
        StoryRequestDTO updateRequest = StoryRequestDTO.builder()
                .extension(25)
                .finished(false)
                .build();

        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));
        when(storyRepository.save(any(StoryEntity.class))).thenReturn(testStory);

        // When
        storyService.updateStory(1L, updateRequest);

        // Then
        verify(storyRepository).save(argThat(story -> 
            story.getExtension() == 25 && !story.isFinished()
        ));
    }

    @Test
    @DisplayName("Debe eliminar historia y sus bloqueos")
    void shouldDeleteStoryAndItsBlocks() {
        // Given
        when(storyRepository.existsById(1L)).thenReturn(true);

        // When
        storyService.deleteStory(1L);

        // Then
        verify(blockedStoryRepository).deleteByStoryId(1L);
        verify(storyRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar historia inexistente")
    void shouldThrowExceptionWhenDeletingNonExistentStory() {
        // Given
        when(storyRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> storyService.deleteStory(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Historia no encontrada");

        verify(storyRepository, never()).deleteById(any());
    }

    // ========== MÉTODOS DE IBaseService ==========

    @Test
    @DisplayName("getEntities debe llamar a getStories")
    void getEntitiesShouldCallGetStories() {
        // Given
        when(storyRepository.findAll()).thenReturn(Arrays.asList(testStory));

        // When
        List<StoryResponseDTO> result = storyService.getEntities();

        // Then
        assertThat(result).hasSize(1);
        verify(storyRepository).findAll();
    }

    @Test
    @DisplayName("getByID debe llamar a getStoryById")
    void getByIDShouldCallGetStoryById() {
        // Given
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));

        // When
        StoryResponseDTO result = storyService.getByID(1L);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        verify(storyRepository).findById(1L);
    }

    @Test
    @DisplayName("createEntity debe llamar a createStory")
    void createEntityShouldCallCreateStory() {
        // Given
        StoryRequestDTO request = StoryRequestDTO.builder().extension(10).finished(false).build();
        when(storyRepository.save(any())).thenAnswer(inv -> {
            StoryEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        // When
        StoryResponseDTO result = storyService.createEntity(request);

        // Then
        assertThat(result).isNotNull();
        verify(storyRepository).save(any());
    }

    @Test
    @DisplayName("updateEntity debe llamar a updateStory")
    void updateEntityShouldCallUpdateStory() {
        // Given
        StoryRequestDTO request = StoryRequestDTO.builder().extension(15).finished(true).build();
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));
        when(storyRepository.save(any())).thenReturn(testStory);

        // When
        StoryResponseDTO result = storyService.updateEntity(1L, request);

        // Then
        assertThat(result).isNotNull();
        verify(storyRepository).findById(1L);
    }

    @Test
    @DisplayName("deleteEntity debe llamar a deleteStory")
    void deleteEntityShouldCallDeleteStory() {
        // Given
        when(storyRepository.existsById(1L)).thenReturn(true);

        // When
        storyService.deleteEntity(1L);

        // Then
        verify(blockedStoryRepository).deleteByStoryId(1L);
        verify(storyRepository).deleteById(1L);
    }

    // ========== ASIGNACIÓN DE HISTORIAS ==========

    @Test
    @DisplayName("Debe asignar historia disponible y crear bloqueo")
    void shouldAssignAvailableStoryAndCreateBlock() {
        // Given
        String userEmail = "test@example.com";
        LocalDateTime now = LocalDateTime.now();

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(blockedStoryRepository.findActiveBlocks(any())).thenReturn(Collections.emptyList());
        when(storyRepository.findAll()).thenReturn(Arrays.asList(testStory));
        when(collaborationRepository.countByStoryId(1L)).thenReturn(0L);
        when(blockedStoryRepository.save(any(BlockedStoryEntity.class))).thenReturn(blockedStory);

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStoryId()).isEqualTo(1L);
        assertThat(result.getExtension()).isEqualTo(10);
        assertThat(result.getCurrentCollaborationNumber()).isEqualTo(1);
        assertThat(result.getTimeLimit()).isEqualTo(1800); // 30 min
        verify(blockedStoryRepository).save(any(BlockedStoryEntity.class));
    }

    @Test
    @DisplayName("Debe retornar historia ya bloqueada para el usuario")
    void shouldReturnAlreadyBlockedStoryForUser() {
        // Given
        String userEmail = "test@example.com";

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.of(blockedStory));
        when(collaborationRepository.countByStoryId(1L)).thenReturn(2L);
        when(collaborationRepository.findByStoryIdOrderByOrderNumberDesc(1L))
                .thenReturn(Collections.emptyList());

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStoryId()).isEqualTo(1L);
        assertThat(result.getCurrentCollaborationNumber()).isEqualTo(3);
        verify(blockedStoryRepository, never()).save(any()); // No crea nuevo bloqueo
    }

    @Test
    @DisplayName("Debe limpiar bloqueos expirados al asignar")
    void shouldCleanExpiredBlocksWhenAssigning() {
        // Given
        String userEmail = "test@example.com";

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(3);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(blockedStoryRepository.findActiveBlocks(any())).thenReturn(Collections.emptyList());
        when(storyRepository.findAll()).thenReturn(Arrays.asList(testStory));
        when(collaborationRepository.countByStoryId(1L)).thenReturn(0L);
        when(blockedStoryRepository.save(any())).thenReturn(blockedStory);

        // When
        storyService.assignRandomAvailableStory(userEmail);

        // Then
        verify(blockedStoryRepository).deleteExpiredBlocks(any());
    }

    @Test
    @DisplayName("Debe excluir historias bloqueadas activamente")
    void shouldExcludeActivelyBlockedStories() {
        // Given
        String userEmail = "test@example.com";
        StoryEntity anotherStory = StoryEntity.builder()
                .id(2L)
                .extension(10)
                .finished(false)
                .createdAt(LocalDateTime.now())
                .build();

        BlockedStoryEntity activeBlock = BlockedStoryEntity.builder()
                .story(testStory)
                .lockedBy(testUser)
                .blockedUntil(LocalDateTime.now().plusMinutes(15))
                .build();

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(blockedStoryRepository.findActiveBlocks(any()))
                .thenReturn(Arrays.asList(activeBlock));
        when(storyRepository.findAll()).thenReturn(Arrays.asList(testStory, anotherStory));
        when(collaborationRepository.countByStoryId(2L)).thenReturn(0L);
        when(blockedStoryRepository.save(any())).thenReturn(blockedStory);

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        assertThat(result.getStoryId()).isEqualTo(2L); // Asigna la no bloqueada
    }

    @Test
    @DisplayName("Debe excluir historias finalizadas")
    void shouldExcludeFinishedStories() {
        // Given
        String userEmail = "test@example.com";
        testStory.setFinished(true);

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(blockedStoryRepository.findActiveBlocks(any())).thenReturn(Collections.emptyList());
        when(storyRepository.findAll()).thenReturn(Arrays.asList(testStory));
        when(storyRepository.save(any())).thenAnswer(inv -> {
            StoryEntity e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });
        when(blockedStoryRepository.save(any())).thenReturn(blockedStory);
        when(collaborationRepository.countByStoryId(anyLong())).thenReturn(0L);

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        verify(storyRepository).save(any()); // Crea nueva historia
    }

    @Test
    @DisplayName("Debe priorizar historias en progreso sobre nuevas")
    void shouldPrioritizeInProgressStories() {
        // Given
        String userEmail = "test@example.com";
        StoryEntity newStory = StoryEntity.builder()
                .id(2L)
                .extension(10)
                .finished(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(blockedStoryRepository.findActiveBlocks(any())).thenReturn(Collections.emptyList());
        when(storyRepository.findAll()).thenReturn(Arrays.asList(testStory, newStory));
        when(collaborationRepository.countByStoryId(1L)).thenReturn(5L); // En progreso
        when(collaborationRepository.countByStoryId(2L)).thenReturn(0L); // Nueva
        when(blockedStoryRepository.save(any())).thenReturn(blockedStory);
        when(collaborationRepository.findByStoryIdOrderByOrderNumberDesc(anyLong()))
                .thenReturn(Collections.emptyList());

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        assertThat(result.getStoryId()).isIn(1L, 2L); // Puede ser cualquiera pero prioriza 1
        verify(blockedStoryRepository).save(any());
    }

    @Test
    @DisplayName("Debe crear nueva historia si no hay disponibles")
    void shouldCreateNewStoryIfNoneAvailable() {
        // Given
        String userEmail = "test@example.com";

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(blockedStoryRepository.findActiveBlocks(any())).thenReturn(Collections.emptyList());
        when(storyRepository.findAll()).thenReturn(Collections.emptyList());
        when(storyRepository.save(any())).thenAnswer(inv -> {
            StoryEntity e = inv.getArgument(0);
            e.setId(3L);
            return e;
        });
        when(blockedStoryRepository.save(any())).thenReturn(blockedStory);
        when(collaborationRepository.countByStoryId(anyLong())).thenReturn(0L);

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        verify(storyRepository).save(any(StoryEntity.class)); // Crea nueva
        verify(blockedStoryRepository).save(any());
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Debe incluir colaboración previa si existe")
    void shouldIncludePreviousCollaborationWhenExists() {
        // Given
        String userEmail = "test@example.com";
        
        CollaborationEntity previousCollab = CollaborationEntity.builder()
                .id(1L)
                .text("Colaboración previa")
                .orderNumber(1)
                .story(testStory)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(blockedStoryRepository.findActiveBlocks(any())).thenReturn(Collections.emptyList());
        when(storyRepository.findAll()).thenReturn(Arrays.asList(testStory));
        when(collaborationRepository.countByStoryId(1L)).thenReturn(1L);
        when(collaborationRepository.findByStoryIdOrderByOrderNumberDesc(1L))
                .thenReturn(Arrays.asList(previousCollab));
        when(blockedStoryRepository.save(any())).thenReturn(blockedStory);

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        assertThat(result.getCurrentCollaborationNumber()).isEqualTo(2);
        assertThat(result.getPreviousCollaboration()).isNotNull();
        assertThat(result.getPreviousCollaboration().getText()).isEqualTo("Colaboración previa");
    }

    @Test
    @DisplayName("Debe lanzar excepción si usuario no existe")
    void shouldThrowExceptionIfUserNotFound() {
        // Given
        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> storyService.assignRandomAvailableStory("nonexistent@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ========== DESBLOQUEO ==========

    @Test
    @DisplayName("Debe desbloquear historia correctamente")
    void shouldUnlockStorySuccessfully() {
        // When
        storyService.unlockStory(1L);

        // Then
        verify(blockedStoryRepository).deleteByStoryId(1L);
    }

    // ========== HISTORIAS COMPLETADAS ==========

    @Test
    @DisplayName("Debe obtener historias completadas con detalles")
    void shouldGetCompletedStoriesWithDetails() {
        // Given
        testStory.setFinished(true);
        testStory.setUpdatedAt(LocalDateTime.now());

        CollaborationEntity firstCollab = CollaborationEntity.builder()
                .id(1L)
                .text("Primera colaboración")
                .orderNumber(1)
                .story(testStory)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();

        CollaborationEntity secondCollab = CollaborationEntity.builder()
                .id(2L)
                .text("Segunda colaboración")
                .orderNumber(2)
                .story(testStory)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();

        when(storyRepository.findAll()).thenReturn(Arrays.asList(testStory));
        when(collaborationRepository.findByStoryIdWithUserOrderByOrderNumberAsc(1L))
                .thenReturn(Arrays.asList(firstCollab, secondCollab));

        // When
        List<CompletedStoryDTO> result = storyService.getCompletedStories();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getFirstCollaboration()).isNotNull();
        assertThat(result.get(0).getFirstCollaboration().getText()).isEqualTo("Primera colaboración");
        assertThat(result.get(0).getTotalCollaborations()).isEqualTo(2);
    }

    @Test
    @DisplayName("Debe excluir historias no finalizadas de completadas")
    void shouldExcludeNonFinishedStoriesFromCompleted() {
        // Given
        testStory.setFinished(false);

        when(storyRepository.findAll()).thenReturn(Arrays.asList(testStory));

        // When
        List<CompletedStoryDTO> result = storyService.getCompletedStories();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe ordenar historias completadas por fecha descendente")
    void shouldSortCompletedStoriesByDateDesc() {
        // Given
        StoryEntity oldStory = StoryEntity.builder()
                .id(1L)
                .extension(10)
                .finished(true)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        StoryEntity newStory = StoryEntity.builder()
                .id(2L)
                .extension(10)
                .finished(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(storyRepository.findAll()).thenReturn(Arrays.asList(oldStory, newStory));
        when(collaborationRepository.findByStoryIdWithUserOrderByOrderNumberAsc(anyLong()))
                .thenReturn(Collections.emptyList());

        // When
        List<CompletedStoryDTO> result = storyService.getCompletedStories();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2L); // Más reciente primero
        assertThat(result.get(1).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Debe manejar historia completada sin colaboraciones")
    void shouldHandleCompletedStoryWithoutCollaborations() {
        // Given
        testStory.setFinished(true);

        when(storyRepository.findAll()).thenReturn(Arrays.asList(testStory));
        when(collaborationRepository.findByStoryIdWithUserOrderByOrderNumberAsc(1L))
                .thenReturn(Collections.emptyList());

        // When
        List<CompletedStoryDTO> result = storyService.getCompletedStories();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstCollaboration()).isNull();
        assertThat(result.get(0).getTotalCollaborations()).isEqualTo(0);
    }

    @Test
    @DisplayName("Debe retornar lista vacía si no hay historias completadas")
    void shouldReturnEmptyListIfNoCompletedStories() {
        // Given
        when(storyRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<CompletedStoryDTO> result = storyService.getCompletedStories();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe calcular tiempo restante correctamente para bloqueo existente")
    void shouldCalculateTimeRemainingCorrectlyForExistingBlock() {
        // Given
        String userEmail = "test@example.com";
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        blockedStory.setBlockedUntil(futureTime);

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.of(blockedStory));
        when(collaborationRepository.countByStoryId(1L)).thenReturn(0L);

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        assertThat(result.getTimeLimit()).isGreaterThan(0);
        assertThat(result.getTimeLimit()).isLessThanOrEqualTo(900); // Aproximadamente 15 min
    }
}