package dev.lin.exquis.collaboration;

import dev.lin.exquis.collaboration.dtos.CollaborationRequestDTO;
import dev.lin.exquis.collaboration.dtos.CollaborationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api-endpoint}/collaborations")
@RequiredArgsConstructor
public class CollaborationController {

    private final CollaborationService collaborationService;

    // 🔹 Obtener todas las colaboraciones (solo para pruebas o administración)
    @GetMapping
    public List<CollaborationResponseDTO> getAllCollaborations() {
        return collaborationService.getEntities().stream()
                .map(CollaborationResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // 🔹 Obtener una colaboración por ID
    @GetMapping("/{id}")
    public CollaborationResponseDTO getCollaborationById(@PathVariable Long id) {
        CollaborationEntity collaboration = collaborationService.getByID(id);
        return CollaborationResponseDTO.fromEntity(collaboration);
    }

    // 🔹 Crear una nueva colaboración (se llama al enviar la aportación)
    @PostMapping
    public CollaborationResponseDTO createCollaboration(
            @RequestBody CollaborationRequestDTO request,
            Authentication authentication) {
        
        String username = authentication.getName();
        CollaborationEntity created = collaborationService.createCollaboration(request, username);
        return CollaborationResponseDTO.fromEntity(created);
    }

    // 🔹 Actualizar colaboración (rara vez usado, pero mantenido por coherencia)
    @PutMapping("/{id}")
    public CollaborationResponseDTO updateCollaboration(
            @PathVariable Long id,
            @RequestBody CollaborationRequestDTO request) {
        
        CollaborationEntity updated = collaborationService.updateCollaboration(id, request);
        return CollaborationResponseDTO.fromEntity(updated);
    }

    // 🔹 Eliminar colaboración
    @DeleteMapping("/{id}")
    public void deleteCollaboration(@PathVariable Long id) {
        collaborationService.deleteEntity(id);
    }

    // 🔹 Obtener todas las colaboraciones de una historia específica
    @GetMapping("/story/{storyId}")
    public List<CollaborationResponseDTO> getCollaborationsByStory(@PathVariable Long storyId) {
        return collaborationService.getCollaborationsByStory(storyId).stream()
                .map(CollaborationResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}