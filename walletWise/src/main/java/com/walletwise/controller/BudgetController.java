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
import javafx.scene.layout.StackPane;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
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
        limitCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("৳%.2f", data.getValue().getLimit())));
        spentCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("৳%.2f", data.getValue().getSpent())));

        remainingCol.setCellValueFactory(data -> {
            double remaining = data.getValue().getRemaining();
            return new SimpleStringProperty(remaining < 0 ? String.format("-৳%.2f", Math.abs(remaining)) : String.format("৳%.2f", remaining));
        });

        progressCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getProgressRatio()).asObject());
        progressCol.setCellFactory(column -> new TableCell<BudgetDTO, Double>() {
            private final ProgressBar progressBar = new ProgressBar();
            private final Label label = new Label();
            private final StackPane stackPane = new StackPane(progressBar, label);

            {
                progressBar.setMaxWidth(Double.MAX_VALUE);
                progressBar.setPrefHeight(18);
                label.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Double progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || progress == null) {
                    setGraphic(null);
                } else {
                    progressBar.setProgress(Math.min(progress, 1.0));
                    label.setText(String.format("%.1f%%", progress * 100));

                    if (progress >= 1.0) {
                        progressBar.setStyle("-fx-accent: #e74c3c;");
                    } else if (progress >= 0.8) {
                        progressBar.setStyle("-fx-accent: #f39c12;");
                    } else {
                        progressBar.setStyle("-fx-accent: #27ae60;");
                    }
                    setGraphic(stackPane);
                }
            }
        });
    }

    // --- NEW HELPER METHOD TO PREVENT DUPLICATES ---
    private void addUniqueCategory(List<String> list, String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) return;
        String cleanName = rawName.substring(0, 1).toUpperCase() + rawName.substring(1).toLowerCase().trim();
        for (String existing : list) {
            if (existing.equalsIgnoreCase(cleanName)) return;
        }
        list.add(cleanName);
    }

    // --- UPDATED TO SCAN TRANSACTIONS TOO ---
    private void loadCategories() {
        List<String> expenseCategories = new ArrayList<>();

        // 1. Scan Category Table
        for (Category c : categoryDAO.getAll()) {
            if (c.getType().equalsIgnoreCase("Expense")) {
                addUniqueCategory(expenseCategories, c.getName());
            }
        }

        // 2. Scan Transaction Table (Catches orphaned categories like "Study product")
        for (Transaction t : transactionDAO.getAll()) {
            if (t.getType() != null && t.getType().equalsIgnoreCase("Expense")) {
                addUniqueCategory(expenseCategories, t.getCategoryName());
            }
        }

        categoryCombo.setItems(FXCollections.observableArrayList(expenseCategories));
    }

    private void loadBudgetData() {
        try {
            ObservableList<BudgetDTO> displayList = FXCollections.observableArrayList();
            List<Budget> budgets = budgetDAO.getAll();
            LocalDate now = LocalDate.now();

            List<Transaction> currentMonthTxs = transactionDAO.getAll().stream()
                    .filter(t -> t.getType() != null && t.getType().equalsIgnoreCase("Expense"))
                    .filter(t -> {
                        LocalDate d = t.getDate();
                        return d != null && d.getMonth() == now.getMonth() && d.getYear() == now.getYear();
                    })
                    .collect(Collectors.toList());

            for (Budget b : budgets) {
                double spent = currentMonthTxs.stream()
                        .filter(t -> t.getCategoryName() != null && t.getCategoryName().equalsIgnoreCase(b.getCategoryName()))
                        .mapToDouble(Transaction::getAmount)
                        .sum();
                displayList.add(new BudgetDTO(b.getCategoryName(), b.getLimitAmount(), spent));
            }
            budgetTable.setItems(displayList);
            budgetTable.refresh();
        } catch (Exception e) {
            showMessage("Crash loading data: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleSetBudget() {
        try {
            String rawCategory = categoryCombo.getValue();
            if (rawCategory == null || rawCategory.trim().isEmpty() || limitField.getText().isEmpty()) {
                showMessage("Select or type a category and enter a limit.", false);
                return;
            }
            String category = rawCategory.substring(0, 1).toUpperCase() + rawCategory.substring(1).toLowerCase().trim();
            double limit = Double.parseDouble(limitField.getText());

            boolean exists = false;
            for (String item : categoryCombo.getItems()) {
                if (item.equalsIgnoreCase(category)) { exists = true; break; }
            }
            if (!exists) {
                categoryDAO.add(new Category(category, "Expense"));
                categoryCombo.getItems().add(category);
            }

            budgetDAO.saveOrUpdate(new Budget(category, limit));
            limitField.clear();
            showMessage("Budget saved!", true);
            loadBudgetData();
        } catch (NumberFormatException e) {
            showMessage("Invalid amount format.", false);
        } catch (SQLException e) {
            showMessage("Database Error: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleRemoveBudget() {
        BudgetDTO selected = budgetTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                budgetDAO.delete(selected.getCategory());
                loadBudgetData();
                showMessage("Budget removed.", true);
            } catch (SQLException e) {
                showMessage("Database Error: " + e.getMessage(), false);
            }
        } else {
            showMessage("Select a budget from the table to remove.", false);
        }
    }

    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.setStyle(success ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e74c3c;");
    }

    public static class BudgetDTO {
        private final String category;
        private final double limit;
        private final double spent;
        public BudgetDTO(String category, double limit, double spent) {
            this.category = category; this.limit = limit; this.spent = spent;
        }
        public String getCategory() { return category; }
        public double getLimit() { return limit; }
        public double getSpent() { return spent; }
        public double getRemaining() { return limit - spent; }
        public double getProgressRatio() {
            if (limit <= 0) return 0;
            return spent / limit;
        }
    }
}