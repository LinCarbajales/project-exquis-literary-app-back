package dev.lin.exquis.collaboration.dtos;

import dev.lin.exquis.collaboration.CollaborationEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaborationResponseDTO {

    private Long id;
    private String text;
    private Integer orderNumber;
    private LocalDateTime createdAt;
    private Long storyId;
    private UserBasicDTO user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserBasicDTO {
        private Long id;
        private String username;
        private String name;
        private String surname;
    }

    // Método estático para convertir desde Entity a DTO
    public static CollaborationResponseDTO fromEntity(CollaborationEntity entity) {
        return CollaborationResponseDTO.builder()
                .id(entity.getId())
                .text(entity.getText())
                .orderNumber(entity.getOrderNumber())
                .createdAt(entity.getCreatedAt())
                .storyId(entity.getStory().getId())
                .user(UserBasicDTO.builder()
                        .id(entity.getUser().getId())
                        .username(entity.getUser().getUsername())
                        .name(entity.getUser().getName())
                        .surname(entity.getUser().getSurname())
                        .build())
                .build();
    }
}