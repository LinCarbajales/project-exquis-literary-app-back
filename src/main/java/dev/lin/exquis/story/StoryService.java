package dev.lin.exquis.story;

import java.util.List;
import dev.lin.exquis.implementations.IStoryService;
import dev.lin.exquis.story.dtos.StoryRequestDTO;
import dev.lin.exquis.story.dtos.StoryResponseDTO;

public interface StoryService extends IStoryService<StoryResponseDTO, StoryRequestDTO> {
    List<StoryResponseDTO> getStories();
    StoryResponseDTO getStoryById(Long id);
    StoryResponseDTO createStory(StoryRequestDTO dto);
    StoryResponseDTO updateStory(Long id, StoryRequestDTO dto);
    void deleteStory(Long id);
}