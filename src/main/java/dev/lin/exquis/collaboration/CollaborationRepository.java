package dev.lin.exquis.collaboration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


@Repository
public interface CollaborationRepository extends JpaRepository<CollaborationEntity, Long> {
    
    List<CollaborationEntity> findByStoryIdOrderByOrderNumberAsc(Long storyId);
    long countByStoryId(Long storyId);
    List<CollaborationEntity> findByStoryIdOrderByOrderNumberDesc(Long storyId);
    
    @Query("""
        SELECT c FROM CollaborationEntity c
        JOIN FETCH c.user
        WHERE c.story.id = :storyId
        ORDER BY c.orderNumber ASC
    """)
    List<CollaborationEntity> findByStoryIdWithUserOrderByOrderNumberAsc(@Param("storyId") Long storyId);

    @Query("""
        SELECT c FROM CollaborationEntity c
        WHERE c.user.id = :userId AND c.story.id = :storyId
        ORDER BY c.orderNumber DESC
        LIMIT 1
    """)
    Optional<CollaborationEntity> findLastByUserAndStory(@Param("userId") Long userId, @Param("storyId") Long storyId);

    @Query("SELECT COALESCE(MAX(c.orderNumber), 0) FROM CollaborationEntity c WHERE c.story.id = :storyId")
    Integer findMaxOrderNumberByStory(@Param("storyId") Long storyId);

    Optional<CollaborationEntity> findTopByUserIdAndStoryIdOrderByOrderNumberDesc(Long userId, Long storyId);

}
