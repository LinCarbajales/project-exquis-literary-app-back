package dev.lin.exquis.user;

import dev.lin.exquis.user.dtos.UserRequestDTO;
import dev.lin.exquis.user.dtos.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("${api-endpoint}/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public UserResponseDTO register(@RequestBody UserRequestDTO dto) {
        return userService.registerUser(dto);
    }

    @GetMapping
    public List<UserResponseDTO> getAll() {
        return userService.getEntities();
    }

    @GetMapping("/{id}")
    public UserResponseDTO getById(@PathVariable Long id) {
        return userService.getByID(id);
    }

    @PutMapping("/{id}")
    public UserResponseDTO update(@PathVariable Long id, @RequestBody UserRequestDTO dto) {
        return userService.updateEntity(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.deleteEntity(id);
    }

    // 👇 ENDPOINTS para el usuario autenticado
    // IMPORTANTE: principal.getName() devuelve el EMAIL (porque SecurityUser.getUsername() devuelve el email)
    @GetMapping("/me")
    public UserResponseDTO getCurrentUser(Principal principal) {
        System.out.println("🔍 /me llamado por: " + principal.getName());
        // Cambiar a getByEmail en lugar de getByUsername
        return userService.getByEmail(principal.getName());
    }

    @PutMapping("/me")
    public UserResponseDTO updateCurrentUser(Principal principal, @RequestBody UserRequestDTO dto) {
        System.out.println("🔍 /me update llamado por: " + principal.getName());
        return userService.updateByEmail(principal.getName(), dto);
    }

    @DeleteMapping("/me")
    public void deleteCurrentUser(Principal principal) {
        System.out.println("🔍 /me delete llamado por: " + principal.getName());
        userService.deleteByEmail(principal.getName());
    }
}