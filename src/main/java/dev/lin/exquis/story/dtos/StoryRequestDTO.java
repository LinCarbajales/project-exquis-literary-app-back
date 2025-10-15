package dev.lin.exquis.story.dtos;

public record StoryRequestDTO(
    Integer extension,
    Boolean finished,
    String visibility
) {
}