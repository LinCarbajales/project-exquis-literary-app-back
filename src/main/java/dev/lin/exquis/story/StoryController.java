package dev.lin.exquis.story;

import dev.lin.exquis.story.dtos.StoryRequestDTO;
import dev.lin.exquis.story.dtos.StoryResponseDTO;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;

@RestController
@RequestMapping("${api-endpoint}/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @GetMapping
    public List<StoryResponseDTO> getAllStories() {
        return storyService.getStories();
    }

    @GetMapping("/{id}")
    public StoryResponseDTO getStory(@PathVariable Long id) {
        return storyService.getStoryById(id);
    }

    @PostMapping
    public StoryResponseDTO createStory(@RequestBody StoryRequestDTO dto) {
        return storyService.createStory(dto);
    }

    @PutMapping("/{id}")
    public StoryResponseDTO updateStory(@PathVariable Long id, @RequestBody StoryRequestDTO dto) {
        return storyService.updateStory(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteStory(@PathVariable Long id) {
        storyService.deleteStory(id);
    }

    @PostMapping("/assign")
    public ResponseEntity<StoryEntity> assignStoryToUser(@AuthenticationPrincipal UserDetails userDetails) {
        StoryEntity story = storyService.assignRandomAvailableStory(userDetails.getUsername());
        return ResponseEntity.ok(story);
    }

    @PostMapping("/unlock/{storyId}")
    public ResponseEntity<Void> unlockStory(@PathVariable Long storyId) {
        storyService.unlockStory(storyId);
        return ResponseEntity.ok().build();
    }
}