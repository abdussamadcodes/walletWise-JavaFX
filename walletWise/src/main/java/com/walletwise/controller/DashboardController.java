package com.walletwise.controller;

import com.walletwise.dao.TransactionDAO;
import com.walletwise.model.Transaction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;

import java.util.List;

public class DashboardController {

    @FXML private Label balanceLabel;
    @FXML private Label incomeLabel;
    @FXML private Label expenseLabel;
    @FXML private PieChart pieChart;

    private final TransactionDAO transactionDAO = new TransactionDAO();

    @FXML
    public void initialize() {
        loadDashboardData();
    }

    private void loadDashboardData() {
        // Fetch all transactions from our SQLite Database
        List<Transaction> transactions = transactionDAO.getAll();

        double totalIncome = 0;
        double totalExpense = 0;

        // Calculate totals
        for (Transaction t : transactions) {
            if (t.getType().equalsIgnoreCase("Income")) {
                totalIncome += t.getAmount();
            } else if (t.getType().equalsIgnoreCase("Expense")) {
                totalExpense += t.getAmount();
            }
        }

        double balance = totalIncome - totalExpense;

        // Update UI Labels
        balanceLabel.setText(String.format("৳%.2f", balance));
        incomeLabel.setText(String.format("৳%.2f", totalIncome));
        expenseLabel.setText(String.format("৳%.2f", totalExpense));

        // Update PieChart
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Income", totalIncome),
                new PieChart.Data("Expenses", totalExpense)
        );
        pieChart.setData(pieChartData);
    }
}