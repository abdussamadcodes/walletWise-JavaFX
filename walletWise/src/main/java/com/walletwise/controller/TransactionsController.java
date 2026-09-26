package com.walletwise.controller;

import com.walletwise.dao.CategoryDAO;
import com.walletwise.dao.TransactionDAO;
import com.walletwise.model.Category;
import com.walletwise.model.Transaction;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransactionsController {

    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, Integer> idCol;
    @FXML private TableColumn<Transaction, String> dateCol;
    @FXML private TableColumn<Transaction, String> typeCol;
    @FXML private TableColumn<Transaction, String> categoryCol;
    @FXML private TableColumn<Transaction, String> amountCol;
    @FXML private TableColumn<Transaction, String> descCol;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> categoryFilterCombo; // NEW Dropdown
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label recordCountLabel;
    @FXML private Label messageLabel;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO(); // NEW DAO
    private ObservableList<Transaction> transactionList;
    private FilteredList<Transaction> filteredData;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @FXML
    public void initialize() {
        setupTableColumns();

        makeDatePickerTypable(startDatePicker);
        makeDatePickerTypable(endDatePicker);

        typeFilterCombo.setItems(FXCollections.observableArrayList("All Types", "Income", "Expense"));
        typeFilterCombo.setValue("All Types");

        // Load categories into the new filter
        loadCategoryFilter();

        loadTransactions();

        // Listeners for live filtering
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        typeFilterCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        categoryFilterCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    // --- UPDATED TO SCAN TRANSACTIONS TOO ---
    private void loadCategoryFilter() {
        List<String> cats = new ArrayList<>();
        cats.add("All Categories");

        // 1. Scan Category Table
        for (Category c : categoryDAO.getAll()) {
            addUniqueCategory(cats, c.getName());
        }

        // 2. Scan Transaction Table
        for (Transaction t : transactionDAO.getAll()) {
            addUniqueCategory(cats, t.getCategoryName());
        }

        categoryFilterCombo.setItems(FXCollections.observableArrayList(cats));
        if (categoryFilterCombo.getValue() == null) {
            categoryFilterCombo.setValue("All Categories");
        }
    }

    // --- NEW HELPER METHOD ---
    private void addUniqueCategory(List<String> list, String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) return;
        String cleanName = rawName.substring(0, 1).toUpperCase() + rawName.substring(1).toLowerCase().trim();
        for (String existing : list) {
            if (existing.equalsIgnoreCase(cleanName)) return;
        }
        list.add(cleanName);
    }

    private void makeDatePickerTypable(DatePicker picker) {
        picker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? dateFormatter.format(date) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.trim().isEmpty()) {
                    try { return LocalDate.parse(string.trim(), dateFormatter); }
                    catch (Exception e) { return picker.getValue(); }
                }
                return null;
            }
        });

        picker.getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    String text = picker.getEditor().getText();
                    if (text == null || text.trim().isEmpty()) {
                        picker.setValue(null);
                    } else {
                        picker.setValue(LocalDate.parse(text.trim(), dateFormatter));
                    }
                } catch (Exception e) {
                    picker.getEditor().setText(picker.getConverter().toString(picker.getValue()));
                }
            }
        });
    }

    private void setupTableColumns() {
        idCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
        dateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getDate();
            return new SimpleStringProperty(date != null ? dateFormatter.format(date) : "");
        });
        typeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType()));
        categoryCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategoryName()));
        amountCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("৳%.2f", cellData.getValue().getAmount())));
        descCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
    }

    private void loadTransactions() {
        List<Transaction> list = transactionDAO.getAll();
        transactionList = FXCollections.observableArrayList(list);

        filteredData = new FilteredList<>(transactionList, b -> true);
        SortedList<Transaction> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(transactionTable.comparatorProperty());

        transactionTable.setItems(sortedData);
        updateRecordCount();
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String filterType = typeFilterCombo.getValue();
        String filterCategory = categoryFilterCombo.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        filteredData.setPredicate(transaction -> {
            if (filterType != null && !filterType.equals("All Types") && !transaction.getType().equals(filterType)) return false;

            // NEW: Filter by Category Dropdown
            if (filterCategory != null && !filterCategory.equals("All Categories") && !transaction.getCategoryName().equalsIgnoreCase(filterCategory)) return false;

            LocalDate txDate = transaction.getDate();
            if (startDate != null && txDate.isBefore(startDate)) return false;
            if (endDate != null && txDate.isAfter(endDate)) return false;

            if (searchText.isEmpty()) return true;

            if (String.valueOf(transaction.getId()).equals(searchText)) return true;
            if (transaction.getDescription() != null && transaction.getDescription().toLowerCase().contains(searchText)) return true;
            // Removed category from text search since we have a dedicated dropdown now!

            return false;
        });

        updateRecordCount();
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        typeFilterCombo.setValue("All Types");
        categoryFilterCombo.setValue("All Categories");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
    }

    private void updateRecordCount() {
        recordCountLabel.setText(filteredData.size() + " records found");
    }

    private void openTransactionForm(Transaction transaction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TransactionForm.fxml"));
            Parent root = loader.load();

            TransactionFormController controller = loader.getController();
            if (transaction != null) {
                controller.setTransaction(transaction);
            }

            Stage stage = new Stage();
            stage.setTitle(transaction == null ? "Add Transaction" : "Edit Transaction");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            loadTransactions();
            loadCategoryFilter(); // REFRESH filter in case they added a new category!
            messageLabel.setText("");
            applyFilters();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error opening form.");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void handleAdd() { openTransactionForm(null); }

    @FXML
    private void handleEdit() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openTransactionForm(selected);
        } else {
            messageLabel.setText("Please select a transaction to edit.");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void handleDelete() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            transactionDAO.delete(selected.getId());
            transactionList.remove(selected);
            messageLabel.setText("Transaction deleted successfully.");
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            updateRecordCount();
        } else {
            messageLabel.setText("Please select a transaction to delete first.");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }
}