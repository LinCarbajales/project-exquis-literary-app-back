package dev.lin.exquis.blockedStory.exceptions;

public class StoryAlreadyBlockedException extends RuntimeException {
    public StoryAlreadyBlockedException(Long id) {
        super("La historia con ID " + id + " ya está bloqueada.");
    }
}