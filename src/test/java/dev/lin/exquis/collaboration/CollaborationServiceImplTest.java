package dev.lin.exquis.collaboration;

import dev.lin.exquis.story.StoryEntity;
import dev.lin.exquis.story.StoryRepository;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import dev.lin.exquis.collaboration.dtos.CollaborationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollaborationServiceImplTest {

    @Mock
    private CollaborationRepository collaborationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoryRepository storyRepository;

    @InjectMocks
    private CollaborationServiceImpl collaborationService;

    private UserEntity mockUser;
    private StoryEntity mockStory;
    private CollaborationRequestDTO mockRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockUser = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        mockStory = StoryEntity.builder()
                .id(10L)
                .extension(10)
                .finished(false)
                .visibility("private")
                .build();

        mockRequest = new CollaborationRequestDTO();
        mockRequest.setStoryId(10L);
        mockRequest.setText("Un misterioso silencio envolvía el pasillo mientras avanzaba lentamente.");
    }

    @Test
    void testCreateCollaboration_SuccessfulCreation() {
        // 🔹 Configurar mocks
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(storyRepository.findById(10L)).thenReturn(Optional.of(mockStory));
        when(collaborationRepository.countByStoryId(10L)).thenReturn(3L);

        CollaborationEntity savedEntity = CollaborationEntity.builder()
                .id(100L)
                .text(mockRequest.getText())
                .orderNumber(4)
                .story(mockStory)
                .user(mockUser)
                .createdAt(LocalDateTime.now())
                .build();

        when(collaborationRepository.save(any(CollaborationEntity.class))).thenReturn(savedEntity);

        // 🔹 Ejecutar método
        CollaborationEntity result = collaborationService.createCollaboration(mockRequest, "test@example.com");

        // 🔹 Verificaciones
        assertNotNull(result);
        assertEquals(mockRequest.getText(), result.getText());
        assertEquals(4, result.getOrderNumber()); // 3 previas + 1
        assertEquals(mockUser, result.getUser());
        assertEquals(mockStory, result.getStory());
        verify(collaborationRepository).save(any(CollaborationEntity.class));
    }

    @Test
    void testCreateCollaboration_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        mockRequest.setStoryId(10L);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                collaborationService.createCollaboration(mockRequest, "missing@example.com")
        );

        assertTrue(exception.getMessage().contains("Usuario no encontrado"));
        verify(userRepository).findByEmail("missing@example.com");
        verifyNoInteractions(storyRepository);
    }

    @Test
    void testCreateCollaboration_StoryNotFound_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(storyRepository.findById(999L)).thenReturn(Optional.empty());
        mockRequest.setStoryId(999L);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                collaborationService.createCollaboration(mockRequest, "test@example.com")
        );

        assertTrue(exception.getMessage().contains("Historia no encontrada"));
        verify(storyRepository).findById(999L);
        verify(collaborationRepository, never()).save(any());
    }
}
