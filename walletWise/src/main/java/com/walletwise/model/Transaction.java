package com.walletwise.model;

import java.time.LocalDate;

public class Transaction {

    private int id;
    private LocalDate date;
    private TransactionType type;
    private String category;
    private double amount;
    private String description;
    private String receiptPath;


    public Transaction(int id, LocalDate date, TransactionType type, String category,
                       double amount, String description, String receiptPath) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.receiptPath = receiptPath;
    }


    public Transaction(LocalDate date, TransactionType type, String category,
                       double amount, String description, String receiptPath) {
        this(0, date, type, category, amount, description, receiptPath);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }

    @Override
    public String toString() {
        return date + " " + type + " " + category + " " + amount;
    }
}