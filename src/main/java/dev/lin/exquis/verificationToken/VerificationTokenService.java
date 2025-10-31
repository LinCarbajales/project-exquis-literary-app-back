package dev.lin.exquis.verificationToken;

import dev.lin.exquis.user.UserEntity;
import dev.lin.exquis.user.UserRepository;
import dev.lin.exquis.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationTokenService {
    
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    /**
     * Crear y enviar token de verificación
     */
    @Transactional
    public void createAndSendVerificationToken(UserEntity user) {
        log.info("📧 Creando token de verificación para: {}", user.getEmail());
        
        // Eliminar tokens antiguos del usuario
        verificationTokenRepository.findByUser(user)
            .ifPresent(oldToken -> {
                log.info("🗑️ Eliminando token antiguo");
                verificationTokenRepository.delete(oldToken);
            });
        
        // Crear nuevo token
        String tokenValue = UUID.randomUUID().toString();
        VerificationTokenEntity token = new VerificationTokenEntity(tokenValue, user);
        verificationTokenRepository.save(token);
        
        log.info("✅ Token creado y guardado");
        
        // Enviar email
        try {
            emailService.sendVerificationEmail(user.getEmail(), tokenValue, user.getUsername());
            log.info("✅ Email de verificación enviado a: {}", user.getEmail());
        } catch (Exception e) {
            log.error("❌ Error al enviar email: {}", e.getMessage());
            throw new RuntimeException("Error al enviar email de verificación");
        }
    }
    
    /**
     * Verificar token y activar usuario
     */
    @Transactional
    public UserEntity verifyToken(String tokenValue) {
        log.info("🔍 Verificando token: {}...", tokenValue.substring(0, Math.min(8, tokenValue.length())));
        
        VerificationTokenEntity token = verificationTokenRepository.findByToken(tokenValue)
            .orElseThrow(() -> {
                log.error("❌ Token no encontrado");
                return new RuntimeException("Token no válido");
            });
        
        if (token.isExpired()) {
            log.error("❌ Token expirado");
            throw new RuntimeException("El token ha expirado. Solicita uno nuevo.");
        }
        
        if (token.isUsed()) {
            log.error("❌ Token ya utilizado");
            throw new RuntimeException("El token ya fue utilizado");
        }
        
        UserEntity user = token.getUser();
        user.setEmailVerified(true);
        token.setUsed(true);
        
        verificationTokenRepository.save(token);
        userRepository.save(user);
        
        log.info("✅ Email verificado para usuario: {}", user.getUsername());
        
        return user;
    }
}
