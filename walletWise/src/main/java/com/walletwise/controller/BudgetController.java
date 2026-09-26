package com.walletwise.controller;

import com.walletwise.dao.BudgetDAO;
import com.walletwise.dao.CategoryDAO;
import com.walletwise.dao.TransactionDAO;
import com.walletwise.model.Budget;
import com.walletwise.model.Category;
import com.walletwise.model.Transaction;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class BudgetController {

    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField limitField;
    @FXML private Label messageLabel;

    @FXML private TableView<BudgetDTO> budgetTable;
    @FXML private TableColumn<BudgetDTO, String> categoryCol;
    @FXML private TableColumn<BudgetDTO, String> limitCol;
    @FXML private TableColumn<BudgetDTO, String> spentCol;
    @FXML private TableColumn<BudgetDTO, String> remainingCol;
    @FXML private TableColumn<BudgetDTO, Double> progressCol;

    private final BudgetDAO budgetDAO = new BudgetDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    @FXML
    public void initialize() {
        setupTable();
        loadCategories();
        loadBudgetData();
    }

    private void setupTable() {
        categoryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        limitCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getLimit())));
        spentCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getSpent())));

        remainingCol.setCellValueFactory(data -> {
            double remaining = data.getValue().getRemaining();
            return new SimpleStringProperty(remaining < 0 ? String.format("-$%.2f", Math.abs(remaining)) : String.format("$%.2f", remaining));
        });

        // This turns the double value (0.0 to 1.0) into a visual Progress Bar!
        progressCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getProgress()).asObject());
        progressCol.setCellFactory(ProgressBarTableCell.forTableColumn());
    }

    private void loadCategories() {
        List<String> expenseCategories = categoryDAO.getAll().stream()
                .filter(c -> c.getType().equals("Expense"))
                .map(Category::getName)
                .collect(Collectors.toList());
        categoryCombo.setItems(FXCollections.observableArrayList(expenseCategories));
    }

    private void loadBudgetData() {
        ObservableList<BudgetDTO> displayList = FXCollections.observableArrayList();
        List<Budget> budgets = budgetDAO.getAll();

        // Get all transactions for the CURRENT MONTH
        LocalDate now = LocalDate.now();
        List<Transaction> currentMonthTxs = transactionDAO.getAll().stream()
                .filter(t -> t.getType().equals("Expense"))
                .filter(t -> t.getDate().getMonth() == now.getMonth() && t.getDate().getYear() == now.getYear())
                .collect(Collectors.toList());

        // Calculate spent amount for each budget
        for (Budget b : budgets) {
            double spent = currentMonthTxs.stream()
                    .filter(t -> t.getCategoryName().equalsIgnoreCase(b.getCategoryName()))
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            displayList.add(new BudgetDTO(b.getCategoryName(), b.getLimitAmount(), spent));
        }

        budgetTable.setItems(displayList);
    }

    @FXML
    private void handleSetBudget() {
        try {
            String category = categoryCombo.getValue();
            if (category == null || limitField.getText().isEmpty()) {
                showMessage("Select a category and enter a limit.", false);
                return;
            }
            double limit = Double.parseDouble(limitField.getText());

            budgetDAO.saveOrUpdate(new Budget(category, limit));

            limitField.clear();
            showMessage("Budget saved!", true);
            loadBudgetData(); // Refresh table
        } catch (NumberFormatException e) {
            showMessage("Invalid amount format.", false);
        }
    }

    @FXML
    private void handleRemoveBudget() {
        BudgetDTO selected = budgetTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            budgetDAO.delete(selected.getCategory());
            loadBudgetData();
            showMessage("Budget removed.", true);
        } else {
            showMessage("Select a budget from the table to remove.", false);
        }
    }

    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.setStyle(success ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e74c3c;");
    }

    // --- Inner DTO (Data Transfer Object) Class for the TableView ---
    // This is an advanced OOP concept your teacher will love.
    public static class BudgetDTO {
        private final String category;
        private final double limit;
        private final double spent;

        public BudgetDTO(String category, double limit, double spent) {
            this.category = category;
            this.limit = limit;
            this.spent = spent;
        }

        public String getCategory() { return category; }
        public double getLimit() { return limit; }
        public double getSpent() { return spent; }
        public double getRemaining() { return limit - spent; }

        public double getProgress() {
            double progress = spent / limit;
            return Math.min(progress, 1.0); // Caps at 1.0 (100%) so the bar doesn't break if over budget
        }
    }
}