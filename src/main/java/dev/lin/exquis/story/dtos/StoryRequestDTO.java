package dev.lin.exquis.story.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryRequestDTO {
    private Integer extension;
    private boolean finished;
}