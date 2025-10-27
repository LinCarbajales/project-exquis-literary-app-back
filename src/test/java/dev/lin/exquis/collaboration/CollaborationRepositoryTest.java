package dev.lin.exquis.collaboration;

import dev.lin.exquis.role.RoleEntity;
import dev.lin.exquis.role.RoleRepository;
import dev.lin.exquis.story.StoryEntity;
import dev.lin.exquis.story.StoryRepository;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CollaborationRepository - Tests de Queries")
class CollaborationRepositoryTest {

    @Autowired
    private CollaborationRepository collaborationRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private UserEntity testUser;
    private UserEntity anotherUser;
    private StoryEntity testStory;
    private RoleEntity userRole;

    @BeforeEach
    void setUp() {
        collaborationRepository.deleteAll();
        storyRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        userRole = new RoleEntity();
        userRole.setName("USER");
        userRole = roleRepository.save(userRole);

        testUser = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .name("Test")
                .surname("User")
                .password("encoded")
                .roles(Set.of(userRole))
                .build();
        testUser = userRepository.save(testUser);

        anotherUser = UserEntity.builder()
                .username("anotheruser")
                .email("another@example.com")
                .name("Another")
                .surname("User")
                .password("encoded")
                .roles(Set.of(userRole))
                .build();
        anotherUser = userRepository.save(anotherUser);

        testStory = StoryEntity.builder()
                .extension(10)
                .finished(false)
                .createdAt(LocalDateTime.now())
                .build();
        testStory = storyRepository.save(testStory);
    }

    @Test
    @DisplayName("Debe encontrar colaboraciones por historia ordenadas por número")
    void shouldFindByStoryIdOrderedByOrderNumber() {
        // Given
        CollaborationEntity collab1 = createCollaboration("Primera", 1, testStory, testUser);
        CollaborationEntity collab2 = createCollaboration("Segunda", 2, testStory, anotherUser);
        CollaborationEntity collab3 = createCollaboration("Tercera", 3, testStory, testUser);

        collaborationRepository.save(collab3); // Guardar en desorden
        collaborationRepository.save(collab1);
        collaborationRepository.save(collab2);

        // When
        List<CollaborationEntity> result = collaborationRepository.findByStoryIdOrderByOrderNumberAsc(testStory.getId());

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getOrderNumber()).isEqualTo(1);
        assertThat(result.get(1).getOrderNumber()).isEqualTo(2);
        assertThat(result.get(2).getOrderNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("Debe contar colaboraciones por historia")
    void shouldCountByStoryId() {
        // Given
        createAndSaveCollaboration("Primera", 1, testStory, testUser);
        createAndSaveCollaboration("Segunda", 2, testStory, anotherUser);
        createAndSaveCollaboration("Tercera", 3, testStory, testUser);

        // When
        long count = collaborationRepository.countByStoryId(testStory.getId());

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Debe encontrar colaboraciones por historia en orden descendente")
    void shouldFindByStoryIdOrderedDesc() {
        // Given
        createAndSaveCollaboration("Primera", 1, testStory, testUser);
        createAndSaveCollaboration("Segunda", 2, testStory, anotherUser);
        createAndSaveCollaboration("Tercera", 3, testStory, testUser);

        // When
        List<CollaborationEntity> result = collaborationRepository.findByStoryIdOrderByOrderNumberDesc(testStory.getId());

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getOrderNumber()).isEqualTo(3);
        assertThat(result.get(1).getOrderNumber()).isEqualTo(2);
        assertThat(result.get(2).getOrderNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe encontrar colaboraciones con usuario cargado (FETCH)")
    void shouldFindByStoryIdWithUserFetched() {
        // Given
        createAndSaveCollaboration("Primera", 1, testStory, testUser);
        createAndSaveCollaboration("Segunda", 2, testStory, anotherUser);

        // When
        List<CollaborationEntity> result = collaborationRepository.findByStoryIdWithUserOrderByOrderNumberAsc(testStory.getId());

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUser().getUsername()).isEqualTo("testuser");
        assertThat(result.get(1).getUser().getUsername()).isEqualTo("anotheruser");
    }

    @Test
    @DisplayName("Debe encontrar última colaboración de usuario en historia")
    void shouldFindLastByUserAndStory() {
        // Given
        createAndSaveCollaboration("Primera del usuario", 1, testStory, testUser);
        createAndSaveCollaboration("De otro usuario", 2, testStory, anotherUser);
        createAndSaveCollaboration("Segunda del usuario", 3, testStory, testUser);

        // When
        Optional<CollaborationEntity> result = collaborationRepository.findLastByUserAndStory(testUser.getId(), testStory.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderNumber()).isEqualTo(3);
        assertThat(result.get().getText()).isEqualTo("Segunda del usuario");
    }

    @Test
    @DisplayName("Debe retornar empty si usuario no tiene colaboraciones en historia")
    void shouldReturnEmptyIfUserHasNoCollaborations() {
        // Given
        createAndSaveCollaboration("De otro usuario", 1, testStory, anotherUser);

        // When
        Optional<CollaborationEntity> result = collaborationRepository.findLastByUserAndStory(testUser.getId(), testStory.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe encontrar número máximo de orden para historia")
    void shouldFindMaxOrderNumberByStory() {
        // Given
        createAndSaveCollaboration("Primera", 1, testStory, testUser);
        createAndSaveCollaboration("Segunda", 2, testStory, anotherUser);
        createAndSaveCollaboration("Tercera", 3, testStory, testUser);

        // When
        Integer maxOrder = collaborationRepository.findMaxOrderNumberByStory(testStory.getId());

        // Then
        assertThat(maxOrder).isEqualTo(3);
    }

    @Test
    @DisplayName("Debe retornar 0 si no hay colaboraciones en historia")
    void shouldReturn0WhenNoCollaborationsExist() {
        // When
        Integer maxOrder = collaborationRepository.findMaxOrderNumberByStory(testStory.getId());

        // Then
        assertThat(maxOrder).isEqualTo(0);
    }

    @Test
    @DisplayName("Debe encontrar última colaboración de usuario usando findTopBy")
    void shouldFindTopByUserIdAndStoryId() {
        // Given
        createAndSaveCollaboration("Primera", 1, testStory, testUser);
        createAndSaveCollaboration("Segunda", 2, testStory, testUser);
        createAndSaveCollaboration("Tercera", 3, testStory, testUser);

        // When
        Optional<CollaborationEntity> result = collaborationRepository.findTopByUserIdAndStoryIdOrderByOrderNumberDesc(
                testUser.getId(), testStory.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("Debe filtrar colaboraciones solo de historia específica")
    void shouldFilterCollaborationsByStory() {
        // Given
        StoryEntity anotherStory = StoryEntity.builder()
                .extension(10)
                .finished(false)
                .createdAt(LocalDateTime.now())
                .build();
        anotherStory = storyRepository.save(anotherStory);

        createAndSaveCollaboration("Historia 1", 1, testStory, testUser);
        createAndSaveCollaboration("Historia 2", 1, anotherStory, testUser);

        // When
        List<CollaborationEntity> resultStory1 = collaborationRepository.findByStoryIdOrderByOrderNumberAsc(testStory.getId());
        List<CollaborationEntity> resultStory2 = collaborationRepository.findByStoryIdOrderByOrderNumberAsc(anotherStory.getId());

        // Then
        assertThat(resultStory1).hasSize(1);
        assertThat(resultStory2).hasSize(1);
        assertThat(resultStory1.get(0).getText()).isEqualTo("Historia 1");
        assertThat(resultStory2.get(0).getText()).isEqualTo("Historia 2");
    }

    @Test
    @DisplayName("Debe manejar múltiples colaboraciones del mismo usuario en historia")
    void shouldHandleMultipleCollaborationsFromSameUser() {
        // Given
        createAndSaveCollaboration("Primera del usuario", 1, testStory, testUser);
        createAndSaveCollaboration("De otro usuario", 2, testStory, anotherUser);
        createAndSaveCollaboration("Segunda del usuario", 3, testStory, testUser);
        createAndSaveCollaboration("Tercera del usuario", 4, testStory, testUser);

        // When
        long count = collaborationRepository.countByStoryId(testStory.getId());

        // Then
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("Debe retornar lista vacía para historia sin colaboraciones")
    void shouldReturnEmptyListForStoryWithoutCollaborations() {
        // When
        List<CollaborationEntity> result = collaborationRepository.findByStoryIdOrderByOrderNumberAsc(testStory.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe preservar orden temporal cuando orderNumber es igual")
    void shouldPreserveTemporalOrderWhenOrderNumberIsEqual() {
        // Given - Aunque en producción no debería pasar, probar el comportamiento
        CollaborationEntity collab1 = createCollaboration("Primera", 1, testStory, testUser);
        collab1.setCreatedAt(LocalDateTime.now().minusHours(2));
        collaborationRepository.save(collab1);

        CollaborationEntity collab2 = createCollaboration("Segunda", 1, testStory, anotherUser);
        collab2.setCreatedAt(LocalDateTime.now().minusHours(1));
        collaborationRepository.save(collab2);

        // When
        List<CollaborationEntity> result = collaborationRepository.findByStoryIdOrderByOrderNumberAsc(testStory.getId());

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCreatedAt()).isBefore(result.get(1).getCreatedAt());
    }

    // Helper methods
    private CollaborationEntity createCollaboration(String text, int orderNumber, StoryEntity story, UserEntity user) {
        return CollaborationEntity.builder()
                .text(text)
                .orderNumber(orderNumber)
                .story(story)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void createAndSaveCollaboration(String text, int orderNumber, StoryEntity story, UserEntity user) {
        collaborationRepository.save(createCollaboration(text, orderNumber, story, user));
    }
}