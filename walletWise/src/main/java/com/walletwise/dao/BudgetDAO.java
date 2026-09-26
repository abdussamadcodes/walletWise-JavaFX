package com.walletwise.dao;

import com.walletwise.model.Budget;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BudgetDAO {
    private final String URL = "jdbc:sqlite:walletwise.db";

    public BudgetDAO() {
        String sql = "CREATE TABLE IF NOT EXISTS budgets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category_name TEXT UNIQUE NOT NULL, " +
                "limit_amount REAL NOT NULL)";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // FIX: Bulletproof Check-and-Save logic
    public void saveOrUpdate(Budget budget) {
        String checkSql = "SELECT count(*) FROM budgets WHERE category_name = ?";
        String updateSql = "UPDATE budgets SET limit_amount = ? WHERE category_name = ?";
        String insertSql = "INSERT INTO budgets(category_name, limit_amount) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(URL)) {
            boolean exists = false;

            // 1. Check if budget for this category already exists
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, budget.getCategoryName());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    exists = true;
                }
            }

            // 2. Either Update or Insert
            if (exists) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setDouble(1, budget.getLimitAmount());
                    updateStmt.setString(2, budget.getCategoryName());
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, budget.getCategoryName());
                    insertStmt.setDouble(2, budget.getLimitAmount());
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Budget> getAll() {
        List<Budget> budgets = new ArrayList<>();
        String sql = "SELECT * FROM budgets";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                budgets.add(new Budget(rs.getInt("id"), rs.getString("category_name"), rs.getDouble("limit_amount")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return budgets;
    }

    public void delete(String categoryName) {
        String sql = "DELETE FROM budgets WHERE category_name = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoryName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}