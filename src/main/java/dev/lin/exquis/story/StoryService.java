package dev.lin.exquis.story;

import dev.lin.exquis.story.dtos.CompletedStoryDTO;
import dev.lin.exquis.story.dtos.StoryAssignmentResponseDTO;
import dev.lin.exquis.story.dtos.StoryRequestDTO;
import dev.lin.exquis.story.dtos.StoryResponseDTO;

import java.util.List;

public interface StoryService {
    
    // CRUD básico
    List<StoryResponseDTO> getEntities();
    StoryResponseDTO getByID(Long id);
    StoryResponseDTO createEntity(StoryRequestDTO dto);
    StoryResponseDTO updateEntity(Long id, StoryRequestDTO dto);
    void deleteEntity(Long id);
    
    // Métodos de historias
    List<StoryResponseDTO> getStories();
    StoryResponseDTO getStoryById(Long id);
    StoryResponseDTO createStory(StoryRequestDTO dto);
    StoryResponseDTO updateStory(Long id, StoryRequestDTO dto);
    void deleteStory(Long id);
    
    // Métodos de asignación y bloqueo
    StoryAssignmentResponseDTO assignRandomAvailableStory(String userEmail);
    void unlockStory(Long storyId);
    
    // ✅ Nuevo: Obtener historias completadas
    List<CompletedStoryDTO> getCompletedStories();
}