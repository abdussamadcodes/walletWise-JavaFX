package com.walletwise.dao;

import com.walletwise.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON;");

            // 1. Categories Table
            // Changed to make 'name' explicitly UNIQUE so it can act as a Foreign Key target
            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT UNIQUE NOT NULL, " +
                    "type TEXT NOT NULL" +
                    ");");

            // 2. Transactions Table
            // Added FOREIGN KEY referencing categories(name)
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "amount REAL NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "category_name TEXT NOT NULL, " +
                    "description TEXT, " +
                    "date TEXT NOT NULL, " +
                    "currency TEXT NOT NULL DEFAULT 'USD', " +
                    "FOREIGN KEY (category_name) REFERENCES categories(name) ON UPDATE CASCADE ON DELETE RESTRICT" +
                    ");");

            // 3. Budgets Table (Renamed to budget_limits to match BudgetDAO)
            // Added FOREIGN KEY referencing categories(name)
            stmt.execute("CREATE TABLE IF NOT EXISTS budget_limits (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "category_name TEXT UNIQUE NOT NULL, " +
                    "limit_amount REAL NOT NULL, " +
                    "FOREIGN KEY (category_name) REFERENCES categories(name) ON UPDATE CASCADE ON DELETE CASCADE" +
                    ");");

            // 4. Settings Table (Renamed to app_settings to match SettingsDAO)
            stmt.execute("CREATE TABLE IF NOT EXISTS app_settings (" +
                    "setting_key TEXT PRIMARY KEY, " +
                    "setting_value TEXT NOT NULL" +
                    ");");

            // Insert default categories to get you started easily
            stmt.execute("INSERT OR IGNORE INTO categories (name, type) VALUES " +
                    "('Salary', 'Income'), " +
                    "('Freelance', 'Income'), " +
                    "('Food', 'Expense'), " +
                    "('Transport', 'Expense'), " +
                    "('Utilities', 'Expense');");

            System.out.println("Database initialized successfully with Foreign Keys.");

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}