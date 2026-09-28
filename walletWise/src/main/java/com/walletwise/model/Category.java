package com.walletwise.model;

public class Category extends BaseEntity {
    private String name;
    private String type; // "Income" or "Expense"

    public Category(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Category(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public String toString() { return name; }
}