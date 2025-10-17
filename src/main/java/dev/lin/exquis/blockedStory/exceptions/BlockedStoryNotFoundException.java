package dev.lin.exquis.blockedStory.exceptions;

public class BlockedStoryNotFoundException extends RuntimeException {
    public BlockedStoryNotFoundException(Long id) {
        super("No se encontró bloqueo para la historia con ID " + id);
    }
}