package dev.lin.exquis.verificationToken;

import dev.lin.exquis.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, Long> {
    
    Optional<VerificationTokenEntity> findByToken(String token);
    
    Optional<VerificationTokenEntity> findByUser(UserEntity user);
    
    void deleteByUser(UserEntity user);
}