package com.walletwise.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // This will create a file named 'walletwise.db' in your project root directory
    private static final String URL = "jdbc:sqlite:walletwise.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}