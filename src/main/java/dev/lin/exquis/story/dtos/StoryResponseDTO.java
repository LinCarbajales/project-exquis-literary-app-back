package dev.lin.exquis.story.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryResponseDTO {
    private Long id;
    private Integer extension;
    private boolean finished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}