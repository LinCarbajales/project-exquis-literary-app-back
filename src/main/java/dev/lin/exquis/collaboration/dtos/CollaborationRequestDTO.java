
package dev.lin.exquis.collaboration.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaborationRequestDTO {

    private String text;
    private Long storyId;

    // El user se asigna automáticamente desde el token
    // El orderNumber se calcula automáticamente en el servicio
}