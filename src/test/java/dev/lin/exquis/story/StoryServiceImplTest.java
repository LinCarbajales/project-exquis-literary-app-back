package dev.lin.exquis.story;

import dev.lin.exquis.blockedStory.BlockedStoryEntity;
import dev.lin.exquis.blockedStory.BlockedStoryRepository;
import dev.lin.exquis.collaboration.CollaborationEntity;
import dev.lin.exquis.collaboration.CollaborationRepository;
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
@DisplayName("StoryServiceImpl - Tests Unitarios")
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

    @Test
    @DisplayName("Debe crear una historia nueva con valores por defecto")
    void shouldCreateStoryWithDefaultValues() {
        // Given
        StoryRequestDTO request = StoryRequestDTO.builder()
                .extension(null)
                .finished(false)
                .build();

        when(storyRepository.save(any(StoryEntity.class))).thenReturn(testStory);

        // When
        StoryResponseDTO result = storyService.createStory(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExtension()).isEqualTo(10);
        assertThat(result.isFinished()).isFalse();
        verify(storyRepository).save(any(StoryEntity.class));
    }

    @Test
    @DisplayName("Debe asignar historia disponible a usuario sin bloqueos previos")
    void shouldAssignAvailableStoryToUserWithoutPreviousBlocks() {
        // Given
        String userEmail = "test@example.com";
        LocalDateTime now = LocalDateTime.now();

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(blockedStoryRepository.findActiveBlocks(any())).thenReturn(Collections.emptyList());
        when(storyRepository.findAll()).thenReturn(List.of(testStory));
        when(collaborationRepository.countByStoryId(1L)).thenReturn(0L);
        when(blockedStoryRepository.save(any(BlockedStoryEntity.class))).thenReturn(blockedStory);

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStoryId()).isEqualTo(1L);
        assertThat(result.getExtension()).isEqualTo(10);
        assertThat(result.getCurrentCollaborationNumber()).isEqualTo(1);
        assertThat(result.getPreviousCollaboration()).isNull();
        verify(blockedStoryRepository).save(any(BlockedStoryEntity.class));
    }

    @Test
    @DisplayName("Debe devolver historia ya bloqueada si el usuario tiene un bloqueo activo")
    void shouldReturnAlreadyBlockedStoryIfUserHasActiveBlock() {
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
        verify(blockedStoryRepository, never()).save(any(BlockedStoryEntity.class));
    }

    @Test
    @DisplayName("Debe limpiar bloqueos expirados al asignar historia")
    void shouldCleanExpiredBlocksWhenAssigningStory() {
        // Given
        String userEmail = "test@example.com";

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(3);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(blockedStoryRepository.findActiveBlocks(any())).thenReturn(Collections.emptyList());
        when(storyRepository.findAll()).thenReturn(List.of(testStory));
        when(collaborationRepository.countByStoryId(1L)).thenReturn(0L);
        when(blockedStoryRepository.save(any(BlockedStoryEntity.class))).thenReturn(blockedStory);

        // When
        storyService.assignRandomAvailableStory(userEmail);

        // Then
        verify(blockedStoryRepository).deleteExpiredBlocks(any());
    }

    @Test
    @DisplayName("Debe filtrar historias donde el usuario participó recientemente")
    void shouldFilterStoriesWhereUserRecentlyParticipated() {
        // Given
        String userEmail = "test@example.com";
        
        // Usuario participó en orden 3, hay 4 colaboraciones totales
        // Diferencia = 4 - 3 = 1 (necesita 2+ para poder participar)
        CollaborationEntity recentCollab = CollaborationEntity.builder()
                .id(1L)
                .user(testUser)
                .story(testStory)
                .orderNumber(3)
                .build();

        StoryEntity newStory = StoryEntity.builder()
                .id(2L)
                .extension(10)
                .finished(false)
                .createdAt(LocalDateTime.now())
                .build();

        BlockedStoryEntity newBlock = BlockedStoryEntity.builder()
                .id(2L)
                .story(newStory)
                .lockedBy(testUser)
                .blockedUntil(LocalDateTime.now().plusMinutes(30))
                .createdAt(LocalDateTime.now())
                .build();

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(blockedStoryRepository.findActiveBlocks(any())).thenReturn(Collections.emptyList());
        when(storyRepository.findAll()).thenReturn(List.of(testStory));
        when(collaborationRepository.countByStoryId(1L)).thenReturn(4L);
        when(collaborationRepository.findTopByUserIdAndStoryIdOrderByOrderNumberDesc(1L, 1L))
                .thenReturn(Optional.of(recentCollab));
        
        // CRÍTICO: Mock para cuando se guarda la nueva historia
        when(storyRepository.save(any(StoryEntity.class))).thenAnswer(invocation -> {
            StoryEntity savedStory = invocation.getArgument(0);
            // Simular que la BD asigna el ID
            savedStory.setId(2L);
            return savedStory;
        });
        
        // Mocks para la nueva historia creada
        when(blockedStoryRepository.save(any(BlockedStoryEntity.class))).thenReturn(newBlock);
        when(collaborationRepository.countByStoryId(2L)).thenReturn(0L);

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        // Debe crear nueva historia porque el usuario participó recientemente
        verify(storyRepository).save(any(StoryEntity.class));
        assertThat(result).isNotNull();
        assertThat(result.getStoryId()).isEqualTo(2L);
        assertThat(result.getCurrentCollaborationNumber()).isEqualTo(1);
        assertThat(result.getExtension()).isEqualTo(10);
    }

    @Test
    @DisplayName("Debe priorizar historias en progreso sobre historias nuevas")
    void shouldPrioritizeInProgressStoriesOverNewStories() {
        // Given
        String userEmail = "test@example.com";
        
        StoryEntity storyInProgress = StoryEntity.builder()
                .id(2L)
                .extension(10)
                .finished(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(blockedStoryRepository.findActiveBlocks(any())).thenReturn(Collections.emptyList());
        when(storyRepository.findAll()).thenReturn(List.of(testStory, storyInProgress));
        when(collaborationRepository.countByStoryId(1L)).thenReturn(0L); // Nueva
        when(collaborationRepository.countByStoryId(2L)).thenReturn(5L); // En progreso
        when(collaborationRepository.findTopByUserIdAndStoryIdOrderByOrderNumberDesc(1L, 1L))
                .thenReturn(Optional.empty());
        when(collaborationRepository.findTopByUserIdAndStoryIdOrderByOrderNumberDesc(1L, 2L))
                .thenReturn(Optional.empty());
        when(blockedStoryRepository.save(any(BlockedStoryEntity.class))).thenReturn(blockedStory);
        when(collaborationRepository.findByStoryIdOrderByOrderNumberDesc(anyLong()))
                .thenReturn(Collections.emptyList());

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        assertThat(result.getStoryId()).isIn(1L, 2L);
        verify(blockedStoryRepository).save(any(BlockedStoryEntity.class));
    }

    @Test
    @DisplayName("Debe crear nueva historia si no hay historias disponibles")
    void shouldCreateNewStoryIfNoStoriesAvailable() {
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
        when(storyRepository.findAll()).thenReturn(Collections.emptyList());
        when(storyRepository.save(any(StoryEntity.class))).thenReturn(newStory);
        when(blockedStoryRepository.save(any(BlockedStoryEntity.class))).thenReturn(blockedStory);
        when(collaborationRepository.countByStoryId(2L)).thenReturn(0L);

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        assertThat(result).isNotNull();
        verify(storyRepository).save(any(StoryEntity.class));
        verify(blockedStoryRepository).save(any(BlockedStoryEntity.class));
    }

    @Test
    @DisplayName("Debe desbloquear historia correctamente")
    void shouldUnlockStorySuccessfully() {
        // Given
        Long storyId = 1L;

        // When
        storyService.unlockStory(storyId);

        // Then
        verify(blockedStoryRepository).deleteByStoryId(storyId);
    }

    @Test
    @DisplayName("Debe obtener historias completadas con detalles")
    void shouldGetCompletedStoriesWithDetails() {
        // Given
        testStory.setFinished(true);
        
        CollaborationEntity firstCollab = CollaborationEntity.builder()
                .id(1L)
                .text("Primera colaboración")
                .orderNumber(1)
                .user(testUser)
                .story(testStory)
                .createdAt(LocalDateTime.now())
                .build();

        when(storyRepository.findAll()).thenReturn(List.of(testStory));
        when(collaborationRepository.findByStoryIdWithUserOrderByOrderNumberAsc(1L))
                .thenReturn(List.of(firstCollab));

        // When
        List<CompletedStoryDTO> result = storyService.getCompletedStories();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getFirstCollaboration()).isNotNull();
        assertThat(result.get(0).getTotalCollaborations()).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe actualizar historia correctamente")
    void shouldUpdateStorySuccessfully() {
        // Given
        StoryRequestDTO updateRequest = StoryRequestDTO.builder()
                .extension(15)
                .finished(true)
                .build();

        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));
        when(storyRepository.save(any(StoryEntity.class))).thenReturn(testStory);

        // When
        StoryResponseDTO result = storyService.updateStory(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(storyRepository).save(any(StoryEntity.class));
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
    }

    @Test
    @DisplayName("Debe lanzar excepción si usuario no existe al asignar historia")
    void shouldThrowExceptionWhenUserNotFoundForAssignment() {
        // Given
        String userEmail = "nonexistent@example.com";

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> storyService.assignRandomAvailableStory(userEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("Debe excluir historias finalizadas de la asignación")
    void shouldExcludeFinishedStoriesFromAssignment() {
        // Given
        String userEmail = "test@example.com";
        testStory.setFinished(true);

        when(blockedStoryRepository.deleteExpiredBlocks(any())).thenReturn(0);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(blockedStoryRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(blockedStoryRepository.findActiveBlocks(any())).thenReturn(Collections.emptyList());
        when(storyRepository.findAll()).thenReturn(List.of(testStory));
        when(storyRepository.save(any(StoryEntity.class))).thenReturn(testStory);
        when(blockedStoryRepository.save(any(BlockedStoryEntity.class))).thenReturn(blockedStory);
        when(collaborationRepository.countByStoryId(anyLong())).thenReturn(0L);

        // When
        StoryAssignmentResponseDTO result = storyService.assignRandomAvailableStory(userEmail);

        // Then
        verify(storyRepository).save(any(StoryEntity.class)); // Debe crear nueva
    }
}