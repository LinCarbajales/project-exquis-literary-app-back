package dev.lin.exquis.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

/**
 * Servicio para gestionar JWT (JSON Web Tokens)
 * - Genera tokens de autenticación
 * - Valida tokens
 * - Extrae información de los tokens
 */
@Service
public class JwtService {

    // Clave secreta para firmar los tokens (cámbiala en producción por una variable de entorno)
    @Value("${jwt.secret:miClaveSecretaSuperSeguraParaJWT12345678901234567890}")
    private String secretKey;

    // Duración del token en milisegundos (24 horas)
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * Extrae el email (username) del token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae una claim específica del token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Genera un token JWT para un usuario
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Genera un token JWT con claims adicionales
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Construye el token JWT
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .claims(extraClaims) // Replaces .setClaims()
                .subject(userDetails.getUsername()) // Replaces .setSubject()
                .issuedAt(new Date(System.currentTimeMillis())) // Replaces .setIssuedAt()
                .expiration(new Date(System.currentTimeMillis() + expiration)) // Replaces .setExpiration()
                .signWith(getSignInKey()) // Remove SignatureAlgorithm parameter
                .compact();
        }

    /**
     * Valida si el token es válido para un usuario
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Verifica si el token ha expirado
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrae la fecha de expiración del token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    
    /**
     * Extrae todas las claims del token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey()) // ⚠️ Reemplaza a .setSigningKey(). Asegúrate de que getSignInKey() devuelva una SecretKey.
                .build() // ⚠️ Construye el parser
                .parseSignedClaims(token) // ⚠️ Reemplaza a .parseClaimsJws()
                .getPayload(); // Reemplaza a .getBody()
    }

    /**
     * Obtiene la clave de firma
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}