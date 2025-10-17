package dev.lin.exquis.implementations;

import java.util.List;

/**
 * Interfaz base genérica para los servicios de entidades.
 * 
 * @param <T> Tipo de entidad principal (por ejemplo, CollaborationEntity)
 * @param <S> Tipo de DTO o entidad usada para crear/actualizar (puede ser el mismo que T)
 */
public interface IBaseService<T, S> {

    List<T> getEntities();
    T getByID(Long id);
    T createEntity(S dto);
    T updateEntity(Long id, S dto);
    void deleteEntity(Long id);
}
