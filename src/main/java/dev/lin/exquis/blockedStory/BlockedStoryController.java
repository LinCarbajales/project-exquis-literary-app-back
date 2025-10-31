package dev.lin.exquis.blockedStory;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/blocked-stories")
@RequiredArgsConstructor
public class BlockedStoryController {

    private final BlockedStoryService blockedStoryService;

    /**
     * 🔒 Bloquea una historia durante 30 minutos para el usuario.
     */
    @PostMapping("/{storyId}/lock/{userId}")
    public Map<String, Object> blockStory(@PathVariable Long storyId, @PathVariable Long userId) {
        BlockedStoryEntity blocked = blockedStoryService.blockStory(storyId, userId);

        LocalDateTime now = LocalDateTime.now();
        long secondsRemaining = Duration.between(now, blocked.getBlockedUntil()).getSeconds();
        if (secondsRemaining < 0) secondsRemaining = 0;

        Map<String, Object> response = new HashMap<>();
        response.put("storyId", blocked.getStory().getId());
        response.put("blockedUntil", blocked.getBlockedUntil());
        response.put("timeRemaining", secondsRemaining); // en segundos

        return response;
    }

    /**
     * 🔍 Consulta si una historia está bloqueada y cuánto le queda.
     */
    @GetMapping("/{storyId}")
    public Map<String, Object> getBlockInfo(@PathVariable Long storyId) {
        return blockedStoryService.getBlockByStoryId(storyId)
                .map(blocked -> {
                    LocalDateTime now = LocalDateTime.now();
                    long secondsRemaining = Duration.between(now, blocked.getBlockedUntil()).getSeconds();
                    if (secondsRemaining < 0) secondsRemaining = 0;

                    Map<String, Object> data = new HashMap<>();
                    data.put("storyId", blocked.getStory().getId());
                    data.put("lockedBy", blocked.getLockedBy().getId());
                    data.put("blockedUntil", blocked.getBlockedUntil());
                    data.put("timeRemaining", secondsRemaining);
                    return data;
                })
                .orElse(Map.of("blocked", false));
    }

    /**
     * 🔓 Desbloquea manualmente la historia (por abandono o envío).
     */
    @DeleteMapping("/{storyId}/unlock")
    public void unblockStory(@PathVariable Long storyId) {
        blockedStoryService.unblockStory(storyId);
    }
}
