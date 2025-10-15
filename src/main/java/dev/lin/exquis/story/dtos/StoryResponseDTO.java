package dev.lin.exquis.story.dtos;

import java.time.LocalDateTime;

public record StoryResponseDTO(
    Long id,
    Integer extension,
    Boolean finished,
    String visibility,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}