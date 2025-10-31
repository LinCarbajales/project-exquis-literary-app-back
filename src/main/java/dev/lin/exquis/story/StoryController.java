package dev.lin.exquis.story;

import dev.lin.exquis.blockedStory.BlockedStoryEntity;
import dev.lin.exquis.story.dtos.CompletedStoryDTO;
import dev.lin.exquis.story.dtos.StoryAssignmentResponseDTO;
import dev.lin.exquis.story.dtos.StoryRequestDTO;
import dev.lin.exquis.story.dtos.StoryResponseDTO;
import dev.lin.exquis.blockedStory.BlockedStoryRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("${api-endpoint}/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final BlockedStoryRepository blockedStoryRepository;

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

    // ✅ Asignar historia disponible al usuario autenticado
    @PostMapping("/assign")
    public ResponseEntity<StoryAssignmentResponseDTO> assignStoryToUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        String userEmail = authentication.getName();
        System.out.println("🔍 Usuario autenticado: " + userEmail);
        
        StoryAssignmentResponseDTO assignment = storyService.assignRandomAvailableStory(userEmail);
        return ResponseEntity.ok(assignment);
    }

    // ✅ Desbloquear historia (cuando el usuario abandona o termina)
    @PostMapping("/unlock/{storyId}")
    public ResponseEntity<Void> unlockStory(@PathVariable Long storyId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        storyService.unlockStory(storyId);
        return ResponseEntity.ok().build();
    }

    // 🔍 DEBUG: Ver historias bloqueadas (temporal - solo para desarrollo)
    @GetMapping("/blocked")
    public ResponseEntity<List<Map<String, Object>>> getBlockedStories() {
        List<BlockedStoryEntity> blocked = blockedStoryRepository.findAll();
    
        List<Map<String, Object>> result = blocked.stream()
            .map(b -> {
                Map<String, Object> info = new HashMap<>();
                info.put("storyId", b.getStory().getId());
                info.put("lockedBy", b.getLockedBy().getEmail());
                info.put("blockedUntil", b.getBlockedUntil().toString());
                info.put("isExpired", b.getBlockedUntil().isBefore(LocalDateTime.now()));
                return info;
            })
            .collect(Collectors.toList());
    
        return ResponseEntity.ok(result);
    }

    @GetMapping("/completed")
    public ResponseEntity<List<CompletedStoryDTO>> getCompletedStories() {
        List<CompletedStoryDTO> completedStories = storyService.getCompletedStories();
        return ResponseEntity.ok(completedStories);
    }
    }