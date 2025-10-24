package dev.lin.exquis.collaboration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;


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

}
