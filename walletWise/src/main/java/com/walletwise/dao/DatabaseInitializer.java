package com.walletwise.dao;

import com.walletwise.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Enable foreign keys for SQLite
            stmt.execute("PRAGMA foreign_keys = ON;");

            // 1. Categories Table
            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " + // 'Income' or 'Expense'
                    "UNIQUE(name, type)" +
                    ");");

            // 2. Transactions Table
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "amount REAL NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "category_name TEXT NOT NULL, " +
                    "description TEXT, " +
                    "date TEXT NOT NULL, " +
                    "currency TEXT NOT NULL DEFAULT 'USD'" +
                    ");");

            // 3. Budgets Table
            stmt.execute("CREATE TABLE IF NOT EXISTS budgets (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "month_year TEXT NOT NULL, " + // Format: YYYY-MM
                    "category_name TEXT NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "UNIQUE(month_year, category_name)" +
                    ");");

            // 4. Settings Table (For optional PIN lock)
            stmt.execute("CREATE TABLE IF NOT EXISTS settings (" +
                    "key TEXT PRIMARY KEY, " +
                    "value TEXT NOT NULL" +
                    ");");

            // Insert default categories to get you started easily
            stmt.execute("INSERT OR IGNORE INTO categories (name, type) VALUES " +
                    "('Salary', 'Income'), " +
                    "('Freelance', 'Income'), " +
                    "('Food', 'Expense'), " +
                    "('Transport', 'Expense'), " +
                    "('Utilities', 'Expense');");

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}