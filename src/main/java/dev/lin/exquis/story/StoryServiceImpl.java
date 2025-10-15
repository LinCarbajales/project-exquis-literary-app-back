package dev.lin.exquis.story;

import dev.lin.exquis.story.dtos.StoryRequestDTO;
import dev.lin.exquis.story.dtos.StoryResponseDTO;
import dev.lin.exquis.story.exceptions.StoryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;

    @Override
    public List<StoryResponseDTO> getEntities() {
        return getStories();
    }

    @Override
    public StoryResponseDTO getByID(Long id) {
        return getStoryById(id);
    }

    @Override
    public StoryResponseDTO createEntity(StoryRequestDTO dto) {
        return createStory(dto);
    }

    @Override
    public StoryResponseDTO updateEntity(Long id, StoryRequestDTO dto) {
        return updateStory(id, dto);
    }

    @Override
    public void deleteEntity(Long id) {
        deleteStory(id);
    }

    @Override
    public List<StoryResponseDTO> getStories() {
        return storyRepository.findAll().stream()
                .map(storyMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public StoryResponseDTO getStoryById(Long id) {
        StoryEntity entity = storyRepository.findById(id)
                .orElseThrow(() -> new StoryNotFoundException(id));
        return storyMapper.toResponseDTO(entity);
    }

    @Override
    public StoryResponseDTO createStory(StoryRequestDTO dto) {
        StoryEntity entity = storyMapper.toEntity(dto);
        StoryEntity saved = storyRepository.save(entity);
        return storyMapper.toResponseDTO(saved);
    }

    @Override
    public StoryResponseDTO updateStory(Long id, StoryRequestDTO dto) {
        StoryEntity entity = storyRepository.findById(id)
                .orElseThrow(() -> new StoryNotFoundException(id));
        storyMapper.updateEntityFromDTO(entity, dto);
        StoryEntity updated = storyRepository.save(entity);
        return storyMapper.toResponseDTO(updated);
    }

    @Override
    public void deleteStory(Long id) {
        if (!storyRepository.existsById(id)) {
            throw new StoryNotFoundException(id);
        }
        storyRepository.deleteById(id);
    }
}