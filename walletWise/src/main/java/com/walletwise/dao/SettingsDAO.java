package com.walletwise.dao;

import java.sql.*;

public class SettingsDAO {
    private final String URL = "jdbc:sqlite:walletwise.db";

    public SettingsDAO() {
        String sql = "CREATE TABLE IF NOT EXISTS app_settings (" +
                "setting_key TEXT PRIMARY KEY, " +
                "setting_value TEXT NOT NULL)";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveSetting(String key, String value) {
        String checkSql = "SELECT count(*) FROM app_settings WHERE setting_key = ?";
        String updateSql = "UPDATE app_settings SET setting_value = ? WHERE setting_key = ?";
        String insertSql = "INSERT INTO app_settings(setting_key, setting_value) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(URL)) {
            boolean exists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, key);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) exists = true;
            }

            if (exists) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, value);
                    updateStmt.setString(2, key);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, key);
                    insertStmt.setString(2, value);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getSetting(String key) {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("setting_value");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Returns null if no password is set
    }

    public void deleteSetting(String key) {
        String sql = "DELETE FROM app_settings WHERE setting_key = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}