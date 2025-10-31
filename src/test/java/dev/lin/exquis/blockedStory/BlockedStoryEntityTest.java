package dev.lin.exquis.blockedStory;

import dev.lin.exquis.story.StoryEntity;
import dev.lin.exquis.user.UserEntity;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class BlockedStoryEntityTest {

    @Test
    void testBuilderCreatesEntity() {
        StoryEntity story = StoryEntity.builder().id(1L).build();
        UserEntity user = new UserEntity();
        LocalDateTime blockedUntil = LocalDateTime.now().plusHours(2);

        BlockedStoryEntity entity = BlockedStoryEntity.builder()
                .story(story)
                .lockedBy(user)
                .blockedUntil(blockedUntil)
                .build();

        assertNotNull(entity);
        assertEquals(story, entity.getStory());
        assertEquals(user, entity.getLockedBy());
        assertEquals(blockedUntil, entity.getBlockedUntil());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void testSettersAndGetters() {
        StoryEntity story = new StoryEntity();
        UserEntity user = new UserEntity();
        LocalDateTime until = LocalDateTime.now().plusDays(1);

        BlockedStoryEntity entity = new BlockedStoryEntity();
        entity.setId(100L);
        entity.setStory(story);
        entity.setLockedBy(user);
        entity.setBlockedUntil(until);

        assertEquals(100L, entity.getId());
        assertEquals(story, entity.getStory());
        assertEquals(user, entity.getLockedBy());
        assertEquals(until, entity.getBlockedUntil());
    }
}
