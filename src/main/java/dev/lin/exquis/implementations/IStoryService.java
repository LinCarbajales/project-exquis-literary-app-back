package dev.lin.exquis.implementations;

import java.util.List;

public interface IStoryService<T, S> {
    List<T> getEntities();
    T getByID(Long id);
    T createEntity(S dto);
    T updateEntity(Long id, S dto);
    void deleteEntity(Long id);
}
