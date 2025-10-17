package dev.lin.exquis.blockedStory;

import dev.lin.exquis.blockedStory.exceptions.BlockedStoryNotFoundException;
import dev.lin.exquis.blockedStory.exceptions.StoryAlreadyBlockedException;
import dev.lin.exquis.story.StoryEntity;
import dev.lin.exquis.story.StoryRepository;
import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlockedStoryServiceImpl implements BlockedStoryService {

    private final BlockedStoryRepository blockedStoryRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;

    // 🔹 Métodos específicos del servicio de bloqueo

    @Override
    public BlockedStoryEntity blockStory(Long storyId, Long userId) {
        if (isStoryBlocked(storyId)) {
            throw new StoryAlreadyBlockedException(storyId);
        }

        StoryEntity story = storyRepository.findById(storyId)
                .orElseThrow(() -> new BlockedStoryNotFoundException(storyId));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        BlockedStoryEntity blocked = BlockedStoryEntity.builder()
                .story(story)
                .lockedBy(user)
                .blockedUntil(LocalDateTime.now().plusMinutes(30))
                .build();

        return blockedStoryRepository.save(blocked);
    }

    @Override
    public void unblockStory(Long storyId) {
        blockedStoryRepository.deleteByStoryId(storyId);
    }

    @Override
    public Optional<BlockedStoryEntity> getBlockByStoryId(Long storyId) {
        return blockedStoryRepository.findByStoryId(storyId);
    }

    @Override
    public boolean isStoryBlocked(Long storyId) {
        return blockedStoryRepository.findByStoryId(storyId).isPresent();
    }

    // 🔹 Métodos heredados de IBaseService

    @Override
    public List<BlockedStoryEntity> getEntities() {
        return blockedStoryRepository.findAll();
    }

    @Override
    public BlockedStoryEntity getByID(Long id) {
        return blockedStoryRepository.findById(id)
                .orElseThrow(() -> new BlockedStoryNotFoundException(id));
    }

    @Override
    public BlockedStoryEntity createEntity(BlockedStoryEntity dto) {
        // Generalmente no se usa directamente (usa blockStory en su lugar),
        // pero se implementa para cumplir la interfaz genérica.
        return blockedStoryRepository.save(dto);
    }

    @Override
    public BlockedStoryEntity updateEntity(Long id, BlockedStoryEntity dto) {
        BlockedStoryEntity existing = getByID(id);
        existing.setBlockedUntil(dto.getBlockedUntil());
        existing.setLockedBy(dto.getLockedBy());
        return blockedStoryRepository.save(existing);
    }

    @Override
    public void deleteEntity(Long id) {
        if (!blockedStoryRepository.existsById(id)) {
            throw new BlockedStoryNotFoundException(id);
        }
        blockedStoryRepository.deleteById(id);
    }
}
