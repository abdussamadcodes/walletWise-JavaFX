package com.walletwise.controller;

import com.walletwise.dao.TransactionDAO;
import com.walletwise.model.Transaction;
import com.walletwise.util.CategoryUtil;
import com.walletwise.util.CurrencyUtil;
import javafx.animation.PauseTransition;
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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label recordCountLabel;

    @FXML private HBox messageBox;
    @FXML private Label messageLabel;
    @FXML private Button undoButton;

    private PauseTransition messageTimer;
    private Transaction lastDeletedTransaction = null;

    private final TransactionDAO transactionDAO = new TransactionDAO();
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

        loadCategoryFilter();
        loadTransactions();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        typeFilterCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        categoryFilterCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void loadCategoryFilter() {
        List<String> cats = CategoryUtil.getAllUniqueCategories(true);
        CategoryUtil.makeAutoComplete(categoryFilterCombo, cats);

        if (categoryFilterCombo.getValue() == null || categoryFilterCombo.getValue().trim().isEmpty()) {
            categoryFilterCombo.setValue("All Categories");
        }
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
                    if (text == null || text.trim().isEmpty()) picker.setValue(null);
                    else picker.setValue(LocalDate.parse(text.trim(), dateFormatter));
                } catch (Exception e) {
                    picker.getEditor().setText(picker.getConverter().toString(picker.getValue()));
                }
            }
        });
    }

    private void setupTableColumns() {
        String sym = CurrencyUtil.getCurrencySymbol();
        idCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
        dateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getDate();
            return new SimpleStringProperty(date != null ? dateFormatter.format(date) : "");
        });
        typeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType()));
        categoryCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategoryName()));
        amountCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%s%.2f", sym, cellData.getValue().getAmount())));
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
        String filterCategory = categoryFilterCombo.getValue() == null ? "" : categoryFilterCombo.getValue().trim();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        filteredData.setPredicate(transaction -> {
            if (filterType != null && !filterType.equals("All Types") && !transaction.getType().equals(filterType)) return false;

            if (!filterCategory.isEmpty() && !filterCategory.equalsIgnoreCase("All Categories")) {
                if (!transaction.getCategoryName().equalsIgnoreCase(filterCategory)) return false;
            }

            LocalDate txDate = transaction.getDate();
            if (startDate != null && txDate.isBefore(startDate)) return false;
            if (endDate != null && txDate.isAfter(endDate)) return false;

            if (searchText.isEmpty()) return true;

            if (String.valueOf(transaction.getId()).equals(searchText)) return true;
            if (transaction.getDescription() != null && transaction.getDescription().toLowerCase().contains(searchText)) return true;

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

    private void showToast(String message, String type, boolean showUndoBtn) {
        messageLabel.setText(message);
        undoButton.setVisible(showUndoBtn);
        undoButton.setManaged(showUndoBtn);
        messageBox.setVisible(true);

        if ("SUCCESS".equals(type)) {
            messageBox.setStyle("-fx-background-color: #e8f8f5; -fx-padding: 6 15; -fx-background-radius: 20; -fx-border-color: #27ae60; -fx-border-radius: 20; -fx-border-width: 1;");
            messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        } else if ("DELETE".equals(type)) {
            messageBox.setStyle("-fx-background-color: #fdf2e9; -fx-padding: 6 15; -fx-background-radius: 20; -fx-border-color: #e67e22; -fx-border-radius: 20; -fx-border-width: 1;");
            messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e67e22;");
            undoButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 3 10;");
        } else {
            messageBox.setStyle("-fx-background-color: #fdedec; -fx-padding: 6 15; -fx-background-radius: 20; -fx-border-color: #e74c3c; -fx-border-radius: 20; -fx-border-width: 1;");
            messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        }

        if (messageTimer != null) messageTimer.stop();
        messageTimer = new PauseTransition(Duration.seconds(7));
        messageTimer.setOnFinished(e -> {
            messageBox.setVisible(false);
            if (showUndoBtn) lastDeletedTransaction = null;
        });
        messageTimer.play();
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

            if (controller.isSaved()) {
                if (transaction == null) showToast("Transaction added successfully", "SUCCESS", false);
                else showToast("Transaction updated successfully", "SUCCESS", false);
                loadTransactions();
                loadCategoryFilter();
                applyFilters();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error opening form.", "ERROR", false);
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
            showToast("Please select a transaction to edit.", "ERROR", false);
        }
    }

    @FXML
    private void handleDelete() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            lastDeletedTransaction = selected;
            transactionDAO.delete(selected.getId());
            transactionList.remove(selected);
            updateRecordCount();
            showToast("Transaction deleted", "DELETE", true);
        } else {
            showToast("Please select a transaction to delete first.", "ERROR", false);
        }
    }

    @FXML
    private void handleUndo() {
        if (lastDeletedTransaction != null) {
            transactionDAO.add(lastDeletedTransaction);
            loadTransactions();
            loadCategoryFilter();
            applyFilters();
            showToast("Action undone. Transaction restored.", "SUCCESS", false);
            lastDeletedTransaction = null;
        }
    }
}