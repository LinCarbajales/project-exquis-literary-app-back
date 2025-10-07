package dev.lin.exquis.auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping(path = "${api-endpoint}")
public class AuthController {
    
        @GetMapping("/login")
    public ResponseEntity<AuthResponseDTO> login() {
        
        SecurityContext contextHolder = SecurityContextHolder.getContext();
        Authentication auth = contextHolder.getAuthentication();
        
        AuthResponseDTO authResponse = new AuthResponseDTO("Logged", auth.getName(), auth.getAuthorities().iterator().next().getAuthority());

        return ResponseEntity.ok().body(authResponse);
    }
    
}
