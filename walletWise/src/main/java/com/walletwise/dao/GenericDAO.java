package com.walletwise.dao;

import java.util.List;


public interface GenericDAO<T> {
    void add(T entity);
    List<T> getAll();
    void update(T entity);
    void delete(int id);
}