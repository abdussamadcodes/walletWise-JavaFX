package com.walletwise.controller;

import com.walletwise.dao.BudgetDAO;
import com.walletwise.dao.CategoryDAO;
import com.walletwise.dao.SettingsDAO;
import com.walletwise.dao.TransactionDAO;
import com.walletwise.model.Budget;
import com.walletwise.model.Category;
import com.walletwise.model.Transaction;
import com.walletwise.util.CategoryUtil;
import com.walletwise.util.CurrencyUtil;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

public class BudgetController {

    @FXML private Label titleLabel; // NEW: Dynamic title
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
    private final SettingsDAO settingsDAO = new SettingsDAO(); // NEW

    @FXML
    public void initialize() {
        setupTable();
        loadCategories();
        loadBudgetData();
    }

    private void setupTable() {
        String sym = CurrencyUtil.getCurrencySymbol();

        categoryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        limitCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%s%.2f", sym, data.getValue().getLimit())));
        spentCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%s%.2f", sym, data.getValue().getSpent())));

        remainingCol.setCellValueFactory(data -> {
            double remaining = data.getValue().getRemaining();
            return new SimpleStringProperty(remaining < 0 ? String.format("-%s%.2f", sym, Math.abs(remaining)) : String.format("%s%.2f", sym, remaining));
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

    private void loadCategories() {
        List<String> expenseCategories = CategoryUtil.getExpenseCategories();
        CategoryUtil.makeAutoComplete(categoryCombo, expenseCategories);
    }

    private void loadBudgetData() {
        try {
            ObservableList<BudgetDTO> displayList = FXCollections.observableArrayList();
            List<Budget> budgets = budgetDAO.getAll();

            // --- NEW: FETCH GLOBAL DASHBOARD TIME FILTER ---
            LocalDate today = LocalDate.now();
            LocalDate start = today.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate end = today.with(TemporalAdjusters.lastDayOfMonth());

            String filterType = settingsDAO.getSetting("dashboard_time_filter");
            if (filterType == null) filterType = "This Month";

            if ("Custom Date Range".equals(filterType)) {
                String s = settingsDAO.getSetting("dashboard_start_date");
                String e = settingsDAO.getSetting("dashboard_end_date");
                DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");

                if (s != null && !s.isEmpty()) try { start = LocalDate.parse(s, df); } catch(Exception ignored){}
                if (e != null && !e.isEmpty()) try { end = LocalDate.parse(e, df); } catch(Exception ignored){}

                titleLabel.setText("Budget Planner (Custom Range)");
            } else {
                switch (filterType) {
                    case "This Week":
                        start = today.with(java.time.DayOfWeek.MONDAY);
                        end = today.with(java.time.DayOfWeek.SUNDAY);
                        break;
                    case "This Month":
                        start = today.with(TemporalAdjusters.firstDayOfMonth());
                        end = today.with(TemporalAdjusters.lastDayOfMonth());
                        break;
                    case "Last Month":
                        LocalDate lastMonth = today.minusMonths(1);
                        start = lastMonth.with(TemporalAdjusters.firstDayOfMonth());
                        end = lastMonth.with(TemporalAdjusters.lastDayOfMonth());
                        break;
                    case "This Year":
                        start = today.with(TemporalAdjusters.firstDayOfYear());
                        end = today.with(TemporalAdjusters.lastDayOfYear());
                        break;
                    case "All Time":
                        start = null;
                        end = null;
                        break;
                }
                titleLabel.setText("Budget Planner (" + filterType + ")");
            }

            final LocalDate finalStart = start;
            final LocalDate finalEnd = end;

            // --- FILTER TRANSACTIONS DYNAMICALLY BY DASHBOARD DATES ---
            List<Transaction> currentTxs = transactionDAO.getAll().stream()
                    .filter(t -> t.getType() != null && t.getType().equalsIgnoreCase("Expense"))
                    .filter(t -> {
                        LocalDate d = t.getDate();
                        if (d == null) return false;
                        if (finalStart != null && d.isBefore(finalStart)) return false;
                        if (finalEnd != null && d.isAfter(finalEnd)) return false;
                        return true;
                    })
                    .collect(Collectors.toList());

            for (Budget b : budgets) {
                double spent = currentTxs.stream()
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
            loadBudgetData(); // Refresh calculations automatically
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
                loadBudgetData(); // Refresh calculations automatically
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