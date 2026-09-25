package com.walletwise.model;

import java.time.LocalDate;

public class Transaction extends BaseEntity {
    private double amount;
    private String type;
    private String categoryName;
    private String description;
    private LocalDate date;
    private String currency;

    public Transaction(int id, double amount, String type, String categoryName, String description, LocalDate date, String currency) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.categoryName = categoryName;
        this.description = description;
        this.date = date;
        this.currency = currency;
    }

    public Transaction(double amount, String type, String categoryName, String description, LocalDate date, String currency) {
        this.amount = amount;
        this.type = type;
        this.categoryName = categoryName;
        this.description = description;
        this.date = date;
        this.currency = currency;
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}