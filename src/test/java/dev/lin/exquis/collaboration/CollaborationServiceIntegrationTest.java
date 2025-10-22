package dev.lin.exquis.collaboration;

import dev.lin.exquis.story.StoryEntity;
import dev.lin.exquis.story.StoryRepository;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import dev.lin.exquis.collaboration.dtos.CollaborationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
@ActiveProfiles("test")
class CollaborationServiceIntegrationTest {

    @Autowired
    private CollaborationServiceImpl collaborationService;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private CollaborationRepository collaborationRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity user;
    private StoryEntity story;

    @BeforeEach
    void setUp() {
        collaborationRepository.deleteAll();
        storyRepository.deleteAll();
        userRepository.deleteAll();

        user = UserEntity.builder()
                .username("testuser")
                .name("Test")
                .surname(" User")
                .email("test@example.com")
                .password("1234")
                .build();
        user = userRepository.save(user);

        story = StoryEntity.builder()
                .extension(10)
                .finished(false)
                .visibility("private")
                .build();
        story = storyRepository.save(story);
    }

    @Test
    void testCreateCollaboration_SuccessfulIntegration() {
        // 🔹 Crear primera colaboración
        CollaborationRequestDTO request1 = new CollaborationRequestDTO();
        request1.setText("Primera colaboración de prueba, muy inspirada en un día soleado.");
        request1.setStoryId(story.getId());

        CollaborationEntity first = collaborationService.createCollaboration(request1, user.getEmail());

        assertNotNull(first.getId());
        assertEquals(1, first.getOrderNumber());
        assertEquals(user.getId(), first.getUser().getId());
        assertEquals(story.getId(), first.getStory().getId());
        assertEquals("Primera colaboración de prueba, muy inspirada en un día soleado.", first.getText());

        // 🔹 Crear segunda colaboración
        CollaborationRequestDTO request2 = new CollaborationRequestDTO();
        request2.setText("Segunda colaboración de prueba para comprobar el orden.");
        request2.setStoryId(story.getId());

        CollaborationEntity second = collaborationService.createCollaboration(request2, user.getEmail());

        assertEquals(2, second.getOrderNumber());

        // 🔹 Verificar en la base de datos
        List<CollaborationEntity> collaborations = collaborationRepository.findByStoryIdOrderByOrderNumberAsc(story.getId());
        assertEquals(2, collaborations.size());
        assertEquals("Primera colaboración de prueba, muy inspirada en un día soleado.", collaborations.get(0).getText());
        assertEquals("Segunda colaboración de prueba para comprobar el orden.", collaborations.get(1).getText());
    }

    @Test
    void testCreateCollaboration_UserNotFound_ThrowsException() {
        CollaborationRequestDTO request = new CollaborationRequestDTO();
        request.setText("Colaboración inválida.");
        request.setStoryId(story.getId());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                collaborationService.createCollaboration(request, "inexistente@example.com"));

        assertTrue(ex.getMessage().contains("Usuario no encontrado"));
    }

    @Test
    void testCreateCollaboration_StoryNotFound_ThrowsException() {
        CollaborationRequestDTO request = new CollaborationRequestDTO();
        request.setText("Otra colaboración inválida.");
        request.setStoryId(9999L);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                collaborationService.createCollaboration(request, user.getEmail()));

        assertTrue(ex.getMessage().contains("Historia no encontrada"));
    }
}
