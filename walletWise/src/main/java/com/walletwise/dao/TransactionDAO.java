package com.walletwise.dao;

import com.walletwise.model.Transaction;
import com.walletwise.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO implements GenericDAO<Transaction> {

    // 1. CREATE
    @Override
    public void add(Transaction t) {
        String sql = "INSERT INTO transactions (amount, type, category_name, description, date, currency) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, t.getAmount());
            pstmt.setString(2, t.getType());
            pstmt.setString(3, t.getCategoryName());
            pstmt.setString(4, t.getDescription());
            pstmt.setString(5, t.getDate().toString());
            pstmt.setString(6, t.getCurrency());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 2. READ
    @Override
    public List<Transaction> getAll() {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"),
                        rs.getDouble("amount"),
                        rs.getString("type"),
                        rs.getString("category_name"),
                        rs.getString("description"),
                        LocalDate.parse(rs.getString("date")),
                        rs.getString("currency")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 3. UPDATE
    @Override
    public void update(Transaction t) {
        String sql = "UPDATE transactions SET amount=?, type=?, category_name=?, description=?, date=?, currency=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, t.getAmount());
            pstmt.setString(2, t.getType());
            pstmt.setString(3, t.getCategoryName());
            pstmt.setString(4, t.getDescription());
            pstmt.setString(5, t.getDate().toString());
            pstmt.setString(6, t.getCurrency());
            pstmt.setInt(7, t.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 4. DELETE
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM transactions WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}