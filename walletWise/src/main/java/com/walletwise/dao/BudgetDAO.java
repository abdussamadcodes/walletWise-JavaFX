package com.walletwise.dao;

import com.walletwise.model.Budget;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BudgetDAO {
    private final String URL = "jdbc:sqlite:walletwise.db";

    public BudgetDAO() {

        String sql = "CREATE TABLE IF NOT EXISTS budget_limits (" +
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

    public void saveOrUpdate(Budget budget) throws SQLException {
        String checkSql = "SELECT count(*) FROM budget_limits WHERE category_name = ?";
        String updateSql = "UPDATE budget_limits SET limit_amount = ? WHERE category_name = ?";
        String insertSql = "INSERT INTO budget_limits(category_name, limit_amount) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(URL)) {
            boolean exists = false;

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, budget.getCategoryName());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    exists = true;
                }
            }

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
        }
    }

    public List<Budget> getAll() {
        List<Budget> budgets = new ArrayList<>();
        String sql = "SELECT * FROM budget_limits";
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

    public void delete(String categoryName) throws SQLException {
        String sql = "DELETE FROM budget_limits WHERE category_name = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoryName);
            pstmt.executeUpdate();
        }
    }
}