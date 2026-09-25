package com.walletwise.model;

/**
 * Advanced OOP: Abstract Class.
 * Prevents repeating the 'id' field in every model.
 */
public abstract class BaseEntity {
    protected int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}