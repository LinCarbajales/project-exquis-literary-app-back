package dev.lin.exquis.blockedStory;

import dev.lin.exquis.implementations.IBaseService;
import java.util.Optional;

public interface BlockedStoryService extends IBaseService<BlockedStoryEntity, BlockedStoryEntity> {

    BlockedStoryEntity blockStory(Long storyId, Long userId);
    void unblockStory(Long storyId);
    Optional<BlockedStoryEntity> getBlockByStoryId(Long storyId);
    boolean isStoryBlocked(Long storyId);
}
