package dev.lin.exquis.collaboration;

import dev.lin.exquis.collaboration.dtos.CollaborationRequestDTO;
import dev.lin.exquis.collaboration.exceptions.CollaborationNotFoundException;
import dev.lin.exquis.story.StoryEntity;
import dev.lin.exquis.story.StoryRepository;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CollaborationServiceImpl implements CollaborationService {

    private final CollaborationRepository collaborationRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;

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
        // Buscar el usuario autenticado
        System.out.println(request.getStoryId());
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        // Buscar la historia
        StoryEntity story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new RuntimeException("Historia no encontrada: " + request.getStoryId()));

        // Calcular el siguiente número de orden
        int nextOrder = (int) (collaborationRepository.countByStoryId(request.getStoryId()) + 1);

        // Crear la colaboración
        CollaborationEntity collaboration = CollaborationEntity.builder()
                .text(request.getText())
                .orderNumber(nextOrder)
                .createdAt(LocalDateTime.now())
                .story(story)
                .user(user)
                .build();

        return collaborationRepository.save(collaboration);
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
    public List<CollaborationEntity> getCollaborationsByStory(Long storyId) {
        return collaborationRepository.findByStoryIdOrderByOrderNumberAsc(storyId);
    }
}