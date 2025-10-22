package dev.lin.exquis.blockedStory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedStoryRepository extends JpaRepository<BlockedStoryEntity, Long> {
    
    Optional<BlockedStoryEntity> findByStoryId(Long storyId);
    
    // ✅ Con @Modifying y @Transactional
    @Modifying
    @Transactional
    @Query("DELETE FROM BlockedStoryEntity b WHERE b.story.id = :storyId")
    void deleteByStoryId(@Param("storyId") Long storyId);
    
    // ✅ NUEVO: Buscar por usuario
    @Query("SELECT b FROM BlockedStoryEntity b WHERE b.lockedBy.email = :email")
    Optional<BlockedStoryEntity> findByUserEmail(@Param("email") String email);
    
    // ✅ NUEVO: Limpiar bloqueos expirados
    @Modifying
    @Transactional
    @Query("DELETE FROM BlockedStoryEntity b WHERE b.blockedUntil < :now")
    int deleteExpiredBlocks(@Param("now") LocalDateTime now);
    
    // ✅ NUEVO: Obtener bloqueos vigentes
    @Query("SELECT b FROM BlockedStoryEntity b WHERE b.blockedUntil > :now")
    List<BlockedStoryEntity> findActiveBlocks(@Param("now") LocalDateTime now);
}