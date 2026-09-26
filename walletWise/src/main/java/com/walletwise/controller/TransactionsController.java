package com.walletwise.controller;

import com.walletwise.dao.TransactionDAO;
import com.walletwise.model.Transaction;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

public class TransactionsController {

    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, Integer> idCol;
    @FXML private TableColumn<Transaction, String> dateCol;
    @FXML private TableColumn<Transaction, String> typeCol;
    @FXML private TableColumn<Transaction, String> categoryCol;
    @FXML private TableColumn<Transaction, String> amountCol;
    @FXML private TableColumn<Transaction, String> descCol;

    @FXML private Label messageLabel;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private ObservableList<Transaction> transactionList;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadTransactions();
    }

    private void setupTableColumns() {
        // Teacher Requirement: Advanced OOP (Using Lambdas for cell factories)
        idCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate().toString()));
        typeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType()));
        categoryCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategoryName()));

        // Format the amount column to look like money
        amountCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getAmount()))
        );

        descCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
    }

    private void loadTransactions() {
        List<Transaction> list = transactionDAO.getAll();
        transactionList = FXCollections.observableArrayList(list);
        transactionTable.setItems(transactionList);
    }

    @FXML
    private void handleAdd() {
        messageLabel.setText("Add window coming in Phase 8!");
        messageLabel.setStyle("-fx-text-fill: #27ae60;"); // Green
    }

    @FXML
    private void handleEdit() {
        messageLabel.setText("Edit window coming in Phase 8!");
        messageLabel.setStyle("-fx-text-fill: #f39c12;"); // Orange
    }

    @FXML
    private void handleDelete() {
        // Get the transaction the user clicked on
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();

        if (selected != null) {
            // Delete from database
            transactionDAO.delete(selected.getId());
            // Remove from the visual table
            transactionList.remove(selected);

            messageLabel.setText("Transaction deleted successfully.");
            messageLabel.setStyle("-fx-text-fill: #27ae60;"); // Green
        } else {
            messageLabel.setText("Please select a transaction to delete first.");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;"); // Red
        }
    }
}