package dev.lin.exquis.collaboration.exceptions;

public class CollaborationNotFoundException extends RuntimeException {
    public CollaborationNotFoundException(Long id) {
        super("Colaboración con ID " + id + " no encontrada.");
    }
}
