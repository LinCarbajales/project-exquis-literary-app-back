package dev.lin.exquis.blockedStory;

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
@DisplayName("BlockedStoryRepository - Tests de Queries")
class BlockedStoryRepositoryTest {

    @Autowired
    private BlockedStoryRepository blockedStoryRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private UserEntity testUser;
    private UserEntity anotherUser;
    private StoryEntity testStory;
    private StoryEntity anotherStory;
    private RoleEntity userRole;

    @BeforeEach
    void setUp() {
        blockedStoryRepository.deleteAll();
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

        anotherStory = StoryEntity.builder()
                .extension(10)
                .finished(false)
                .createdAt(LocalDateTime.now())
                .build();
        anotherStory = storyRepository.save(anotherStory);
    }

    @Test
    @DisplayName("Debe encontrar bloqueo por ID de historia")
    void shouldFindByStoryId() {
        // Given
        BlockedStoryEntity blocked = createAndSaveBlock(testStory, testUser, LocalDateTime.now().plusMinutes(30));

        // When
        Optional<BlockedStoryEntity> result = blockedStoryRepository.findByStoryId(testStory.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStory().getId()).isEqualTo(testStory.getId());
        assertThat(result.get().getLockedBy().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Debe eliminar bloqueo por ID de historia")
    void shouldDeleteByStoryId() {
        // Given
        createAndSaveBlock(testStory, testUser, LocalDateTime.now().plusMinutes(30));

        // When
        blockedStoryRepository.deleteByStoryId(testStory.getId());
        blockedStoryRepository.flush();

        // Then
        Optional<BlockedStoryEntity> result = blockedStoryRepository.findByStoryId(testStory.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe encontrar bloqueo por email de usuario")
    void shouldFindByUserEmail() {
        // Given
        createAndSaveBlock(testStory, testUser, LocalDateTime.now().plusMinutes(30));

        // When
        Optional<BlockedStoryEntity> result = blockedStoryRepository.findByUserEmail("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getLockedBy().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Debe retornar empty si usuario no tiene bloqueos")
    void shouldReturnEmptyIfUserHasNoBlocks() {
        // When
        Optional<BlockedStoryEntity> result = blockedStoryRepository.findByUserEmail("test@example.com");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe eliminar bloqueos expirados")
    void shouldDeleteExpiredBlocks() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        createAndSaveBlock(testStory, testUser, now.minusMinutes(10)); // Expirado
        createAndSaveBlock(anotherStory, anotherUser, now.plusMinutes(10)); // Vigente

        // When
        int deletedCount = blockedStoryRepository.deleteExpiredBlocks(now);

        // Then
        assertThat(deletedCount).isEqualTo(1);
        assertThat(blockedStoryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe encontrar bloqueos activos/vigentes")
    void shouldFindActiveBlocks() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        createAndSaveBlock(testStory, testUser, now.minusMinutes(10)); // Expirado
        BlockedStoryEntity activeBlock = createAndSaveBlock(anotherStory, anotherUser, now.plusMinutes(10)); // Vigente

        // When
        List<BlockedStoryEntity> result = blockedStoryRepository.findActiveBlocks(now);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(activeBlock.getId());
        assertThat(result.get(0).getBlockedUntil()).isAfter(now);
    }

    @Test
    @DisplayName("Debe retornar lista vacía si no hay bloqueos activos")
    void shouldReturnEmptyListIfNoActiveBlocks() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        createAndSaveBlock(testStory, testUser, now.minusMinutes(10)); // Expirado

        // When
        List<BlockedStoryEntity> result = blockedStoryRepository.findActiveBlocks(now);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe manejar múltiples bloqueos de diferentes usuarios")
    void shouldHandleMultipleBlocksFromDifferentUsers() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        createAndSaveBlock(testStory, testUser, now.plusMinutes(30));
        createAndSaveBlock(anotherStory, anotherUser, now.plusMinutes(30));

        // When
        List<BlockedStoryEntity> activeBlocks = blockedStoryRepository.findActiveBlocks(now);

        // Then
        assertThat(activeBlocks).hasSize(2);
    }

    @Test
    @DisplayName("Debe eliminar solo bloqueos expirados, manteniendo vigentes")
    void shouldDeleteOnlyExpiredBlocksKeepingActive() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        StoryEntity story1 = storyRepository.save(StoryEntity.builder()
                .extension(10).finished(false).createdAt(now).build());
        StoryEntity story2 = storyRepository.save(StoryEntity.builder()
                .extension(10).finished(false).createdAt(now).build());
        StoryEntity story3 = storyRepository.save(StoryEntity.builder()
                .extension(10).finished(false).createdAt(now).build());

        createAndSaveBlock(story1, testUser, now.minusMinutes(30)); // Expirado
        createAndSaveBlock(story2, testUser, now.minusMinutes(5));  // Expirado
        createAndSaveBlock(story3, anotherUser, now.plusMinutes(20)); // Vigente

        // When
        int deletedCount = blockedStoryRepository.deleteExpiredBlocks(now);

        // Then
        assertThat(deletedCount).isEqualTo(2);
        assertThat(blockedStoryRepository.count()).isEqualTo(1);
        
        List<BlockedStoryEntity> remaining = blockedStoryRepository.findAll();
        assertThat(remaining.get(0).getStory().getId()).isEqualTo(story3.getId());
    }

    @Test
    @DisplayName("Debe encontrar bloqueo en el límite exacto de expiración")
    void shouldHandleBlockAtExactExpirationTime() {
        // Given
        LocalDateTime exactTime = LocalDateTime.now();
        createAndSaveBlock(testStory, testUser, exactTime);

        // When - Buscar justo en el momento de expiración
        List<BlockedStoryEntity> activeBefore = blockedStoryRepository.findActiveBlocks(exactTime.minusSeconds(1));
        List<BlockedStoryEntity> activeAfter = blockedStoryRepository.findActiveBlocks(exactTime.plusSeconds(1));

        // Then
        assertThat(activeBefore).hasSize(1); // Aún vigente 1 segundo antes
        assertThat(activeAfter).isEmpty();   // Después tampoco
    }

    @Test
    @DisplayName("Debe permitir solo un bloqueo por historia")
    void shouldAllowOnlyOneBlockPerStory() {
        // Given
        createAndSaveBlock(testStory, testUser, LocalDateTime.now().plusMinutes(30));

        // When
        Optional<BlockedStoryEntity> result = blockedStoryRepository.findByStoryId(testStory.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(blockedStoryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe encontrar bloqueo correcto cuando usuario bloquea múltiples historias")
    void shouldFindCorrectBlockWhenUserBlocksMultipleStories() {
        // Given
        createAndSaveBlock(testStory, testUser, LocalDateTime.now().plusMinutes(30));
        createAndSaveBlock(anotherStory, testUser, LocalDateTime.now().plusMinutes(30));

        // When
        Optional<BlockedStoryEntity> result = blockedStoryRepository.findByStoryId(testStory.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStory().getId()).isEqualTo(testStory.getId());
    }

    @Test
    @DisplayName("Debe retornar 0 al eliminar bloqueos cuando no hay expirados")
    void shouldReturn0WhenDeletingWithNoExpiredBlocks() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        createAndSaveBlock(testStory, testUser, now.plusMinutes(30)); // Vigente

        // When
        int deletedCount = blockedStoryRepository.deleteExpiredBlocks(now);

        // Then
        assertThat(deletedCount).isEqualTo(0);
        assertThat(blockedStoryRepository.count()).isEqualTo(1);
    }

    // Helper methods
    private BlockedStoryEntity createAndSaveBlock(StoryEntity story, UserEntity user, LocalDateTime blockedUntil) {
        BlockedStoryEntity block = BlockedStoryEntity.builder()
                .story(story)
                .lockedBy(user)
                .blockedUntil(blockedUntil)
                .createdAt(LocalDateTime.now())
                .build();
        return blockedStoryRepository.save(block);
    }
}