package dev.lin.exquis.implementations;

public interface IRegisterService<T, S> {
    S register(T request);
}
