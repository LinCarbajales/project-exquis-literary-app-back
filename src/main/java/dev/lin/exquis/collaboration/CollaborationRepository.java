package dev.lin.exquis.collaboration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CollaborationRepository extends JpaRepository<CollaborationEntity, Long> {
    List<CollaborationEntity> findByStoryIdOrderByOrderNumberAsc(Long storyId);
    long countByStoryId(Long storyId);
}
