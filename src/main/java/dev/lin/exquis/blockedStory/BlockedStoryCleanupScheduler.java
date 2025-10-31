package dev.lin.exquis.blockedStory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service // ✅ puedes usar @Service o @Component, ambos sirven
@RequiredArgsConstructor
@Slf4j
public class BlockedStoryCleanupScheduler {

    private final BlockedStoryRepository blockedStoryRepository;

    /**
     * 🕒 Limpia los bloqueos expirados cada 1 minuto (para pruebas).
     */
    @Scheduled(fixedRate = 60000) // cada 60 segundos
    @Transactional
    public void cleanupExpiredBlocks() {
        LocalDateTime now = LocalDateTime.now();
        
        // 🚨 CAMBIO AQUÍ: Usar el método con el @Query explícito
        int deleted = blockedStoryRepository.deleteExpiredBlocks(now); 
        
        log.info("🧹 Limpieza automática ejecutada a {} — bloqueos eliminados: {}", now, deleted);
    }
}
