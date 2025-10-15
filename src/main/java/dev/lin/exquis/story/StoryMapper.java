package dev.lin.exquis.story;

import dev.lin.exquis.story.dtos.StoryRequestDTO;
import dev.lin.exquis.story.dtos.StoryResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class StoryMapper {

    public StoryEntity toEntity(StoryRequestDTO dto) {
        return StoryEntity.builder()
                .extension(dto.extension())
                .finished(dto.finished())
                .visibility(dto.visibility())
                .build();
    }

    public StoryResponseDTO toResponseDTO(StoryEntity entity) {
        return new StoryResponseDTO(
                entity.getId(),
                entity.getExtension(),
                entity.isFinished(),
                entity.getVisibility(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public void updateEntityFromDTO(StoryEntity entity, StoryRequestDTO dto) {
        entity.setExtension(dto.extension());
        entity.setFinished(dto.finished());
        entity.setVisibility(dto.visibility());
    }
}