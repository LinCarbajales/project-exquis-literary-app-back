package dev.lin.exquis.blockedStory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BlockedStoryRepository extends JpaRepository<BlockedStoryEntity, Long> {
    Optional<BlockedStoryEntity> findByStoryId(Long storyId);
    void deleteByStoryId(Long storyId);
}
