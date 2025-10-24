package dev.lin.exquis.collaboration;

import dev.lin.exquis.collaboration.dtos.CollaborationRequestDTO;
import dev.lin.exquis.collaboration.dtos.CollaborationResponseDTO;
import dev.lin.exquis.collaboration.exceptions.CollaborationNotFoundException;
import dev.lin.exquis.story.StoryEntity;
import dev.lin.exquis.story.StoryRepository;
import dev.lin.exquis.story.StoryService;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CollaborationServiceImpl implements CollaborationService {

    private final CollaborationRepository collaborationRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final StoryService storyService;

    @Override
    public List<CollaborationEntity> getEntities() {
        return collaborationRepository.findAll();
    }

    @Override
    public CollaborationEntity getByID(Long id) {
        return collaborationRepository.findById(id)
                .orElseThrow(() -> new CollaborationNotFoundException(id));
    }

    @Override
    public CollaborationEntity createEntity(CollaborationEntity collaboration) {
        // Asignar el número de orden automáticamente
        int nextOrder = (int) (collaborationRepository.countByStoryId(collaboration.getStory().getId()) + 1);
        collaboration.setOrderNumber(nextOrder);
        return collaborationRepository.save(collaboration);
    }

    @Override
    public CollaborationEntity createCollaboration(CollaborationRequestDTO request, String username) {
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        StoryEntity story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new RuntimeException("Historia no encontrada: " + request.getStoryId()));
        
        System.out.println("=================================>" + request.getStoryId());
        int nextOrder = (int) (collaborationRepository.countByStoryId(request.getStoryId()) + 1);

        CollaborationEntity collaboration = CollaborationEntity.builder()
                .text(request.getText())
                .orderNumber(nextOrder)
                .createdAt(LocalDateTime.now())
                .story(story)
                .user(user)
                .build();

        CollaborationEntity saved = collaborationRepository.save(collaboration);

        // ✅ Verificar si la historia debe marcarse como finalizada
        long totalCollaborations = collaborationRepository.countByStoryId(story.getId());
        if (totalCollaborations >= story.getExtension() && !story.isFinished()) {
            System.out.println("✅ Historia " + story.getId() + " completada: " + totalCollaborations + "/" + story.getExtension());
            story.setFinished(true);
            story.setUpdatedAt(LocalDateTime.now());
            storyRepository.save(story);
        }

        // ✅ Desbloquear historia al enviar la colaboración
        storyService.unlockStory(story.getId());

        return saved;
    }

    @Override
    public CollaborationEntity updateEntity(Long id, CollaborationEntity updated) {
        CollaborationEntity existing = getByID(id);
        existing.setText(updated.getText());
        return collaborationRepository.save(existing);
    }

    @Override
    public CollaborationEntity updateCollaboration(Long id, CollaborationRequestDTO request) {
        CollaborationEntity existing = getByID(id);
        existing.setText(request.getText());
        return collaborationRepository.save(existing);
    }

    @Override
    public void deleteEntity(Long id) {
        if (!collaborationRepository.existsById(id)) {
            throw new CollaborationNotFoundException(id);
        }
        collaborationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollaborationResponseDTO> getCollaborationsByStory(Long storyId) {
        List<CollaborationEntity> collaborations = collaborationRepository
                .findByStoryIdWithUserOrderByOrderNumberAsc(storyId);

        return collaborations.stream()
                .map(CollaborationResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}