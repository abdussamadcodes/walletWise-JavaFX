package com.walletwise.dao;

import java.util.List;

/**
 * Advanced OOP: Interface with Generics.
 * Forces all DAOs to implement standard CRUD operations.
 */
public interface GenericDAO<T> {
    void add(T entity);
    List<T> getAll();
    void update(T entity);
    void delete(int id);
}