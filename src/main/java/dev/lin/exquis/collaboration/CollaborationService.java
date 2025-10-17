package dev.lin.exquis.collaboration;

import dev.lin.exquis.collaboration.dtos.CollaborationRequestDTO;
import dev.lin.exquis.implementations.IBaseService;

import java.util.List;

public interface CollaborationService extends IBaseService<CollaborationEntity, CollaborationEntity> {

    List<CollaborationEntity> getCollaborationsByStory(Long storyId);
    
    CollaborationEntity createCollaboration(CollaborationRequestDTO request, String username);
    
    CollaborationEntity updateCollaboration(Long id, CollaborationRequestDTO request);
}