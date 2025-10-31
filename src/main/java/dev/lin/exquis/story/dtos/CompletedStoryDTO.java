package dev.lin.exquis.story.dtos;

import dev.lin.exquis.collaboration.dtos.CollaborationResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompletedStoryDTO {
    private Long id;
    private Integer extension;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private CollaborationResponseDTO firstCollaboration;
    private Integer totalCollaborations;
}