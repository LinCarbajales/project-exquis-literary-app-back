package dev.lin.exquis.blockedStory;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blocked-stories")
@RequiredArgsConstructor
public class BlockedStoryController {

    private final BlockedStoryService blockedStoryService;

    @PostMapping("/{storyId}/lock/{userId}")
    public BlockedStoryEntity blockStory(@PathVariable Long storyId, @PathVariable Long userId) {
        return blockedStoryService.blockStory(storyId, userId);
    }

    @DeleteMapping("/{storyId}/unlock")
    public void unblockStory(@PathVariable Long storyId) {
        blockedStoryService.unblockStory(storyId);
    }
}
