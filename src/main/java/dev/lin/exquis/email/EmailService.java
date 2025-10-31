package dev.lin.exquis.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

@Service
@Slf4j
public class EmailService {
    
    @Value("${app.frontend.url}")
    private String frontendUrl;
    
    @Value("${gmail.username}")
    private String gmailUsername;
    
    @Value("${gmail.password}")
    private String gmailPassword;
    
    /**
     * Enviar email de verificación usando Gmail
     */
    public void sendVerificationEmail(String toEmail, String token, String username) {
        log.info("📧 Preparando email de verificación para: {}", toEmail);
        
        try {
            String verificationLink = frontendUrl + "/verify-email/" + token;
            
            // Configurar propiedades de Gmail
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            // Crear sesión con autenticación
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(gmailUsername, gmailPassword);
                }
            });
            
            // Crear mensaje
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(gmailUsername, "Exquis"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Verifica tu cuenta en Exquis 🖋️");
            
            // Contenido HTML
            String htmlContent = buildVerificationEmailHtml(username, verificationLink);
            message.setContent(htmlContent, "text/html; charset=utf-8");
            
            // Enviar
            Transport.send(message);
            
            log.info("✅ Email enviado exitosamente a: {}", toEmail);
            
        } catch (Exception e) {
            log.error("❌ Error al enviar email a {}: {}", toEmail, e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al enviar email de verificación: " + e.getMessage());
        }
    }
    
    /**
     * Construir HTML del email de verificación
     */
    private String buildVerificationEmailHtml(String username, String verificationLink) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Verifica tu cuenta</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background-color: #f7fafc;
                        padding: 20px;
                        line-height: 1.6;
                    }
                    .email-container { 
                        max-width: 600px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        padding: 40px 20px;
                        text-align: center;
                        color: white;
                    }
                    .logo { 
                        font-size: 3rem;
                        margin-bottom: 10px;
                    }
                    .header-title {
                        font-size: 1.8rem;
                        font-weight: 600;
                        margin: 0;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    h1 {
                        color: #2d3748;
                        font-size: 1.5rem;
                        margin-bottom: 20px;
                    }
                    p {
                        color: #4a5568;
                        margin-bottom: 15px;
                        font-size: 1rem;
                    }
                    .button-container {
                        text-align: center;
                        margin: 35px 0;
                    }
                    .verify-button {
                        display: inline-block;
                        padding: 16px 40px;
                        background: #6b46c1;
                        color: white;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 1.1rem;
                    }
                    .link-box {
                        background: #f7fafc;
                        padding: 15px;
                        border-radius: 8px;
                        margin: 20px 0;
                        word-break: break-all;
                        font-size: 0.85rem;
                        color: #4a5568;
                        border: 1px solid #e2e8f0;
                    }
                    .warning-box {
                        background: #fff5f5;
                        border-left: 4px solid #f56565;
                        padding: 15px;
                        margin: 25px 0;
                        border-radius: 4px;
                    }
                    .warning-box strong {
                        color: #c53030;
                        display: block;
                        margin-bottom: 5px;
                    }
                    .warning-box p {
                        color: #742a2a;
                        margin: 0;
                        font-size: 0.95rem;
                    }
                    .footer {
                        background: #f7fafc;
                        padding: 30px;
                        text-align: center;
                        border-top: 1px solid #e2e8f0;
                    }
                    .footer p {
                        color: #718096;
                        font-size: 0.85rem;
                        margin: 5px 0;
                    }
                    .divider {
                        height: 1px;
                        background: #e2e8f0;
                        margin: 25px 0;
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <div class="logo">🖋️</div>
                        <h2 class="header-title">Exquis</h2>
                    </div>
                    
                    <div class="content">
                        <h1>¡Hola, %s!</h1>
                        
                        <p>Gracias por registrarte en <strong>Exquis</strong>, la plataforma donde las historias cobran vida a través de la colaboración.</p>
                        
                        <p>Estás a un paso de comenzar a escribir y crear historias extraordinarias. Para activar tu cuenta, solo necesitas verificar tu correo electrónico.</p>
                        
                        <div class="button-container">
                            <a href="%s" class="verify-button">✨ Verificar mi email</a>
                        </div>
                        
                        <div class="divider"></div>
                        
                        <p><strong>¿El botón no funciona?</strong> Copia y pega este enlace en tu navegador:</p>
                        <div class="link-box">%s</div>
                        
                        <div class="warning-box">
                            <strong>⚠️ Importante</strong>
                            <p>Este enlace expirará en <strong>24 horas</strong>. Si no verificas tu cuenta en ese tiempo, deberás solicitar un nuevo enlace.</p>
                        </div>
                        
                        <div class="divider"></div>
                        
                        <p style="font-size: 0.9rem; color: #718096;">Si no te registraste en Exquis, puedes ignorar este mensaje con toda seguridad.</p>
                    </div>
                    
                    <div class="footer">
                        <p><strong>© 2025 Exquis</strong></p>
                        <p>Historias Colaborativas</p>
                        <p style="margin-top: 15px;">Este es un correo automático, por favor no respondas a este mensaje.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, verificationLink, verificationLink);
    }
}