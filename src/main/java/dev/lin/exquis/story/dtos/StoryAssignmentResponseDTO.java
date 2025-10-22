package dev.lin.exquis.story.dtos;

import dev.lin.exquis.collaboration.dtos.CollaborationResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryAssignmentResponseDTO {
    private Long storyId;
    private Integer extension;
    private Integer currentCollaborationNumber;
    private CollaborationResponseDTO previousCollaboration; // null si es la primera
    private Integer timeLimit; // en segundos (ej: 1800 para 30 minutos)
}