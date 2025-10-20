package dev.lin.exquis.story;

import dev.lin.exquis.blockedStory.BlockedStoryEntity;
import dev.lin.exquis.blockedStory.BlockedStoryRepository;
import dev.lin.exquis.story.dtos.StoryRequestDTO;
import dev.lin.exquis.story.dtos.StoryResponseDTO;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final BlockedStoryRepository blockedStoryRepository;
    private final UserRepository userRepository;

    // ---------- CRUD (mapeos a DTOs) ----------

    @Override
    public List<StoryResponseDTO> getStories() {
        return storyRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StoryResponseDTO getStoryById(Long id) {
        StoryEntity entity = storyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Historia no encontrada: " + id));
        return toResponse(entity);
    }

    @Override
    public StoryResponseDTO createStory(StoryRequestDTO dto) {
        StoryEntity entity = StoryEntity.builder()
                .extension(dto.getExtension() != null ? dto.getExtension() : 10)
                .finished(dto.isFinished())
                .visibility(dto.getVisibility() != null ? dto.getVisibility() : "private")
                .createdAt(LocalDateTime.now())
                .build();
        StoryEntity saved = storyRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    public StoryResponseDTO updateStory(Long id, StoryRequestDTO dto) {
        StoryEntity story = storyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Historia no encontrada: " + id));
        if (dto.getExtension() != null) story.setExtension(dto.getExtension());
        story.setFinished(dto.isFinished());
        if (dto.getVisibility() != null) story.setVisibility(dto.getVisibility());
        story.setUpdatedAt(LocalDateTime.now());
        StoryEntity saved = storyRepository.save(story);
        return toResponse(saved);
    }

    @Override
    public void deleteStory(Long id) {
        if (!storyRepository.existsById(id)) {
            throw new RuntimeException("Historia no encontrada: " + id);
        }
        // eliminar bloqueo si existe
        blockedStoryRepository.deleteByStoryId(id);
        storyRepository.deleteById(id);
    }

    // Métodos para cumplir IStoryService (alias a los métodos anteriores)
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

    // ---------- Asignación aleatoria y desbloqueo ----------

    @Override
    public StoryEntity assignRandomAvailableStory(String username) {
        // obtener IDs bloqueados (vigentes)
        Set<Long> blockedIds = blockedStoryRepository.findAll()
                .stream()
                .filter(b -> b.getBlockedUntil().isAfter(LocalDateTime.now()))
                .map(b -> b.getStory().getId())
                .collect(Collectors.toSet());

        // obtener historias no finalizadas
        List<StoryEntity> candidates = storyRepository.findAll()
                .stream()
                .filter(s -> !s.isFinished())
                .filter(s -> !blockedIds.contains(s.getId()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            throw new RuntimeException("No hay historias disponibles para colaborar.");
        }

        // elegir aleatoria
        StoryEntity chosen = candidates.get(new Random().nextInt(candidates.size()));

        // crear bloqueo en blocked_stories
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        BlockedStoryEntity blocked = BlockedStoryEntity.builder()
                .story(chosen)
                .lockedBy(user)
                .blockedUntil(LocalDateTime.now().plusMinutes(30))
                .createdAt(LocalDateTime.now())
                .build();

        blockedStoryRepository.save(blocked);

        // devolver la historia (entidad completa) para que el frontend muestre datos
        return chosen;
    }

    @Override
    public void unlockStory(Long storyId) {
        // eliminar bloqueo (si existe)
        blockedStoryRepository.deleteByStoryId(storyId);
        // no cambiamos el objeto story (no asumimos campo blocked en StoryEntity)
    }

    // ---------- helpers de mapeo ----------

    private StoryResponseDTO toResponse(StoryEntity entity) {
        return StoryResponseDTO.builder()
                .id(entity.getId())
                .extension(entity.getExtension())
                .finished(entity.isFinished())
                .visibility(entity.getVisibility())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
