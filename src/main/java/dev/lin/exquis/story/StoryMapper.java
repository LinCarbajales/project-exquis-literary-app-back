package dev.lin.exquis.story;

import dev.lin.exquis.story.dtos.StoryRequestDTO;
import dev.lin.exquis.story.dtos.StoryResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class StoryMapper {

    public StoryEntity toEntity(StoryRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return StoryEntity.builder()
                .extension(dto.getExtension())
                .finished(dto.isFinished())
                .visibility(dto.getVisibility())
                .build();
    }

    public StoryResponseDTO toResponseDTO(StoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return StoryResponseDTO.builder()
                .id(entity.getId())
                .extension(entity.getExtension())
                .finished(entity.isFinished())
                .visibility(entity.getVisibility())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDTO(StoryEntity entity, StoryRequestDTO dto) {
        if (dto == null || entity == null) return;

        if (dto.getExtension() != null) {
            entity.setExtension(dto.getExtension());
        }
        entity.setFinished(dto.isFinished());
        if (dto.getVisibility() != null) {
            entity.setVisibility(dto.getVisibility());
        }
    }
}
