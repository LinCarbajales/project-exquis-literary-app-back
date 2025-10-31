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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

        // 🧹 Limpiar bloqueos expirados
        int expiredCount = blockedStoryRepository.deleteExpiredBlocks(now);
        if (expiredCount > 0) {
            System.out.println("🧹 Eliminados " + expiredCount + " bloqueos expirados");
        }

        // 👤 Obtener usuario
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userEmail));

        // ⚠️ Verificar si el usuario ya tiene una historia bloqueada
        Optional<BlockedStoryEntity> existingBlock = blockedStoryRepository.findByUserEmail(userEmail);
        if (existingBlock.isPresent()) {
            BlockedStoryEntity block = existingBlock.get();
            StoryEntity blockedStory = block.getStory();

            int currentCollaborationNumber = (int) collaborationRepository.countByStoryId(blockedStory.getId()) + 1;

            CollaborationResponseDTO previousCollaboration = null;
            if (currentCollaborationNumber > 1) {
                List<CollaborationEntity> collaborations = collaborationRepository
                        .findByStoryIdOrderByOrderNumberDesc(blockedStory.getId());
                if (!collaborations.isEmpty()) {
                    previousCollaboration = CollaborationResponseDTO.fromEntity(collaborations.get(0));
                }
            }

            long secondsRemaining = Math.max(0, Duration.between(
                    now.atZone(ZoneId.systemDefault()).toInstant(),
                    block.getBlockedUntil().atZone(ZoneId.systemDefault()).toInstant()
            ).getSeconds());

            return StoryAssignmentResponseDTO.builder()
                    .storyId(blockedStory.getId())
                    .extension(blockedStory.getExtension())
                    .currentCollaborationNumber(currentCollaborationNumber)
                    .previousCollaboration(previousCollaboration)
                    .timeLimit((int) secondsRemaining)
                    .build();
        }

        // 🔒 Obtener historias candidatas
        Set<Long> blockedIds = blockedStoryRepository.findActiveBlocks(now)
                .stream().map(b -> b.getStory().getId()).collect(Collectors.toSet());

        List<StoryEntity> allCandidates = storyRepository.findAll()
                .stream()
                .filter(s -> !s.isFinished() && !blockedIds.contains(s.getId()))
                .collect(Collectors.toList());

        // 🧩 Filtrar historias donde el usuario puede participar (mínimo 2 colaboraciones de otros desde la última)
        List<StoryEntity> availableStories = allCandidates.stream()
                .filter(s -> {
                    Optional<CollaborationEntity> lastUserCollab = collaborationRepository
                            .findTopByUserIdAndStoryIdOrderByOrderNumberDesc(user.getId(), s.getId());

                    if (lastUserCollab.isEmpty()) {
                        // Nunca colaboró → puede participar
                        return true;
                    }

                    long currentTotal = collaborationRepository.countByStoryId(s.getId());
                    int lastUserOrder = lastUserCollab.get().getOrderNumber();

                    boolean canParticipate = currentTotal >= lastUserOrder + 2;

                    if (!canParticipate) {
                        System.out.println("❌ Historia " + s.getId() + " excluida: usuario participó en orden "
                                + lastUserOrder + ", actual: " + currentTotal);
                    }

                    return canParticipate;
                })
                .collect(Collectors.toList());

        // ⚙️ Separar historias según progreso
        List<StoryEntity> storiesInProgress = availableStories.stream()
                .filter(s -> collaborationRepository.countByStoryId(s.getId()) > 0 &&
                        collaborationRepository.countByStoryId(s.getId()) < s.getExtension())
                .collect(Collectors.toList());

        List<StoryEntity> newStories = availableStories.stream()
                .filter(s -> collaborationRepository.countByStoryId(s.getId()) == 0)
                .collect(Collectors.toList());

        // 🧠 Elegir historia disponible o crear una nueva si no hay
        StoryEntity chosen;
        if (!storiesInProgress.isEmpty()) {
            chosen = storiesInProgress.get(new Random().nextInt(storiesInProgress.size()));
        } else if (!newStories.isEmpty()) {
            chosen = newStories.get(new Random().nextInt(newStories.size()));
        } else {
            // Si no hay historias válidas, crear una nueva
            chosen = StoryEntity.builder()
                    .extension(10)
                    .finished(false)
                    .createdAt(now)
                    .build();
            chosen = storyRepository.save(chosen);
        }

        // 🔒 Bloquear historia para el usuario
        BlockedStoryEntity blocked = BlockedStoryEntity.builder()
                .story(chosen)
                .lockedBy(user)
                .blockedUntil(now.plusMinutes(30))
                .createdAt(now)
                .build();
        blockedStoryRepository.save(blocked);

        int currentCollaborationNumber = (int) collaborationRepository.countByStoryId(chosen.getId()) + 1;

        CollaborationResponseDTO previousCollaboration = null;
        if (currentCollaborationNumber > 1) {
            List<CollaborationEntity> collaborations = collaborationRepository
                    .findByStoryIdOrderByOrderNumberDesc(chosen.getId());
            if (!collaborations.isEmpty()) {
                previousCollaboration = CollaborationResponseDTO.fromEntity(collaborations.get(0));
            }
        }

        // 🎯 Tiempo restante: siempre 30 minutos al crear un bloqueo nuevo
        long secondsRemaining = 30 * 60;

        return StoryAssignmentResponseDTO.builder()
                .storyId(chosen.getId())
                .extension(chosen.getExtension())
                .currentCollaborationNumber(currentCollaborationNumber)
                .previousCollaboration(previousCollaboration)
                .timeLimit((int) secondsRemaining)
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