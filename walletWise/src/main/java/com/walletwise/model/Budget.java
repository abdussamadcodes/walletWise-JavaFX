package com.walletwise.model;

public class Budget {
    private int id;
    private String categoryName;
    private double limitAmount;

    public Budget(int id, String categoryName, double limitAmount) {
        this.id = id;
        this.categoryName = categoryName;
        this.limitAmount = limitAmount;
    }

    public Budget(String categoryName, double limitAmount) {
        this.categoryName = categoryName;
        this.limitAmount = limitAmount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public double getLimitAmount() { return limitAmount; }
    public void setLimitAmount(double limitAmount) { this.limitAmount = limitAmount; }
}