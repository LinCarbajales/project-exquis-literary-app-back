package dev.lin.exquis.collaboration;

import dev.lin.exquis.story.StoryEntity;
import dev.lin.exquis.user.UserEntity;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class CollaborationEntityTest {

    @Test
    void testBuilderCreatesEntity() {
        StoryEntity story = StoryEntity.builder().id(10L).build();
        UserEntity user = new UserEntity();

        CollaborationEntity entity = CollaborationEntity.builder()
                .text("Una colaboración de prueba")
                .orderNumber(5)
                .story(story)
                .user(user)
                .build();

        assertNotNull(entity);
        assertEquals("Una colaboración de prueba", entity.getText());
        assertEquals(5, entity.getOrderNumber());
        assertEquals(story, entity.getStory());
        assertEquals(user, entity.getUser());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void testSettersAndGetters() {
        StoryEntity story = new StoryEntity();
        UserEntity user = new UserEntity();
        LocalDateTime createdAt = LocalDateTime.now();

        CollaborationEntity entity = new CollaborationEntity();
        entity.setId(77L);
        entity.setText("Texto");
        entity.setOrderNumber(9);
        entity.setStory(story);
        entity.setUser(user);
        entity.setCreatedAt(createdAt);

        assertEquals(77L, entity.getId());
        assertEquals("Texto", entity.getText());
        assertEquals(9, entity.getOrderNumber());
        assertEquals(story, entity.getStory());
        assertEquals(user, entity.getUser());
        assertEquals(createdAt, entity.getCreatedAt());
    }
}
