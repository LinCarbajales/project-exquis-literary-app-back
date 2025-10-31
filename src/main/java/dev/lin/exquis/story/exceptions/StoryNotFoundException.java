package dev.lin.exquis.story.exceptions;

public class StoryNotFoundException extends RuntimeException {
    public StoryNotFoundException(Long id) {
        super("Historia con ID " + id + " no encontrada.");
    }
}
