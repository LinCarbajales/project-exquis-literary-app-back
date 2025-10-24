package dev.lin.exquis.story;

import dev.lin.exquis.blockedStory.BlockedStoryEntity;
import dev.lin.exquis.blockedStory.BlockedStoryRepository;
import dev.lin.exquis.collaboration.CollaborationEntity;
import dev.lin.exquis.collaboration.CollaborationRepository;
import dev.lin.exquis.collaboration.dtos.CollaborationResponseDTO;
import dev.lin.exquis.story.dtos.CompletedStoryDTO;
import dev.lin.exquis.story.dtos.StoryAssignmentResponseDTO;
import dev.lin.exquis.story.dtos.StoryRequestDTO;
import dev.lin.exquis.story.dtos.StoryResponseDTO;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import dev.lin.exquis.story.dtos.CompletedStoryDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final BlockedStoryRepository blockedStoryRepository;
    private final CollaborationRepository collaborationRepository;
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
        story.setUpdatedAt(LocalDateTime.now());
        StoryEntity saved = storyRepository.save(story);
        return toResponse(saved);
    }

    @Override
    public void deleteStory(Long id) {
        if (!storyRepository.existsById(id)) {
            throw new RuntimeException("Historia no encontrada: " + id);
        }
        blockedStoryRepository.deleteByStoryId(id);
        storyRepository.deleteById(id);
    }

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
@Transactional
public StoryAssignmentResponseDTO assignRandomAvailableStory(String userEmail) {
    System.out.println("🔍 Buscando historia disponible para: " + userEmail);
    LocalDateTime now = LocalDateTime.now();
    
    // 🧹 PASO 1: Limpiar bloqueos expirados
    int expiredCount = blockedStoryRepository.deleteExpiredBlocks(now);
    if (expiredCount > 0) {
        System.out.println("🧹 Eliminados " + expiredCount + " bloqueos expirados");
    }
    
    // 👤 PASO 2: Obtener usuario
    UserEntity user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userEmail));

    // ⚠️ PASO 3: Verificar si el usuario ya tiene una historia bloqueada
    Optional<BlockedStoryEntity> existingBlock = blockedStoryRepository.findByUserEmail(userEmail);
    if (existingBlock.isPresent()) {
        BlockedStoryEntity block = existingBlock.get();
        System.out.println("⚠️ El usuario ya tiene la historia " + block.getStory().getId() + " bloqueada");
        throw new RuntimeException("Ya tienes una historia asignada. Complétala o abandónala primero.");
    }

    // 🔒 PASO 4: Obtener IDs de historias bloqueadas (vigentes)
    Set<Long> blockedIds = blockedStoryRepository.findActiveBlocks(now)
            .stream()
            .map(b -> b.getStory().getId())
            .collect(Collectors.toSet());

    System.out.println("🔒 Historias actualmente bloqueadas: " + blockedIds);

    // 📚 PASO 5: Obtener historias candidatas (no finalizadas y no bloqueadas)
    List<StoryEntity> allCandidates = storyRepository.findAll()
            .stream()
            .filter(s -> !s.isFinished())
            .filter(s -> !blockedIds.contains(s.getId()))
            .collect(Collectors.toList());

    System.out.println("📚 Historias candidatas totales: " + allCandidates.size());

    // 🎯 PASO 6: Priorizar historias en progreso
    List<StoryEntity> storiesInProgress = allCandidates.stream()
            .filter(s -> {
                long collabCount = collaborationRepository.countByStoryId(s.getId());
                return collabCount > 0 && collabCount < s.getExtension();
            })
            .collect(Collectors.toList());

    List<StoryEntity> newStories = allCandidates.stream()
            .filter(s -> collaborationRepository.countByStoryId(s.getId()) == 0)
            .collect(Collectors.toList());

    System.out.println("📝 Historias en progreso: " + storiesInProgress.size());
    System.out.println("🆕 Historias nuevas: " + newStories.size());

    StoryEntity chosen;

    if (!storiesInProgress.isEmpty()) {
        chosen = storiesInProgress.get(new Random().nextInt(storiesInProgress.size()));
        System.out.println("🎯 Historia en progreso elegida: " + chosen.getId());
    } else if (!newStories.isEmpty()) {
        chosen = newStories.get(new Random().nextInt(newStories.size()));
        System.out.println("🆕 Historia nueva elegida: " + chosen.getId());
    } else {
        System.out.println("➕ Creando nueva historia...");
        chosen = StoryEntity.builder()
                .extension(10)
                .finished(false)
                .createdAt(now)
                .build();
        chosen = storyRepository.save(chosen);
        System.out.println("✅ Nueva historia creada con ID: " + chosen.getId());
    }

    // 🔒 PASO 7: Bloquear la historia para el usuario
    BlockedStoryEntity blocked = BlockedStoryEntity.builder()
            .story(chosen)
            .lockedBy(user)
            .blockedUntil(now.plusMinutes(30))
            .createdAt(now)
            .build();

    blockedStoryRepository.save(blocked);
    System.out.println("🔒 Historia " + chosen.getId() + " bloqueada para " + userEmail + " hasta: " + blocked.getBlockedUntil());

    // 📊 PASO 8: Obtener número de colaboración actual
    int currentCollaborationNumber = (int) collaborationRepository.countByStoryId(chosen.getId()) + 1;

    // 📖 PASO 9: Obtener la última colaboración (si existe)
    CollaborationResponseDTO previousCollaboration = null;
    if (currentCollaborationNumber > 1) {
        List<CollaborationEntity> collaborations = collaborationRepository
                .findByStoryIdOrderByOrderNumberDesc(chosen.getId());
        if (!collaborations.isEmpty()) {
            previousCollaboration = CollaborationResponseDTO.fromEntity(collaborations.get(0));
        }
    }

    // 🎯 PASO 10: Construir y devolver respuesta
    return StoryAssignmentResponseDTO.builder()
            .storyId(chosen.getId())
            .extension(chosen.getExtension())
            .currentCollaborationNumber(currentCollaborationNumber)
            .previousCollaboration(previousCollaboration)
            .timeLimit(1800)
            .build();
}

    @Override
    @Transactional
    public void unlockStory(Long storyId) {
        System.out.println("🔓 Desbloqueando historia: " + storyId);
        blockedStoryRepository.deleteByStoryId(storyId);
    }

    // ---------- Helper de mapeo ----------

    private StoryResponseDTO toResponse(StoryEntity entity) {
        return new StoryResponseDTO(
                entity.getId(),
                entity.getExtension(),
                entity.isFinished(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }


    @Override
    @Transactional(readOnly = true)
    public List<CompletedStoryDTO> getCompletedStories() {
        System.out.println("📚 Obteniendo historias completadas...");
    
        // Obtener todas las historias finalizadas y públicas
        List<StoryEntity> finishedStories = storyRepository.findAll()
                .stream()
                .filter(StoryEntity::isFinished)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Más recientes primero
                .collect(Collectors.toList());
    
        System.out.println("✅ Encontradas " + finishedStories.size() + " historias completadas");
    
        // Mapear a CompletedStoryDTO
        return finishedStories.stream()
                .map(story -> {
                    // Obtener la primera colaboración (orden 1)
                    List<CollaborationEntity> collaborations = collaborationRepository
                            .findByStoryIdWithUserOrderByOrderNumberAsc(story.getId());
                
                    CollaborationResponseDTO firstCollab = null;
                    if (!collaborations.isEmpty()) {
                        firstCollab = CollaborationResponseDTO.fromEntity(collaborations.get(0));
                    }
                
                    // Contar total de colaboraciones
                    int totalCollabs = collaborations.size();
                
                    return CompletedStoryDTO.builder()
                            .id(story.getId())
                            .extension(story.getExtension())
                            .createdAt(story.getCreatedAt())
                            .updatedAt(story.getUpdatedAt())
                            .firstCollaboration(firstCollab)
                            .totalCollaborations(totalCollabs)
                            .build();
                })
                .collect(Collectors.toList());
    }
}