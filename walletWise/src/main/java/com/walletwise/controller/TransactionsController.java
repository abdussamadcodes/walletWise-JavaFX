package com.walletwise.controller;

import com.walletwise.dao.TransactionDAO;
import com.walletwise.model.Transaction;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

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

    @FXML private Label messageLabel;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private ObservableList<Transaction> transactionList;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadTransactions();
    }



    private void setupTableColumns() {
        idCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));

        // FIX: Formatting the Table column to show DD-MM-YYYY
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        dateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getDate();
            return new SimpleStringProperty(date != null ? formatter.format(date) : "");
        });

        typeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType()));
        categoryCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategoryName()));
        amountCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getAmount())));
        descCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
    }

    private void loadTransactions() {
        List<Transaction> list = transactionDAO.getAll();
        transactionList = FXCollections.observableArrayList(list);
        transactionTable.setItems(transactionList);
    }

    // --- NEW METHOD: Opens the Add/Edit Popup Window ---
    private void openTransactionForm(Transaction transaction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TransactionForm.fxml"));
            Parent root = loader.load();

            // Pass the selected transaction to the popup controller
            TransactionFormController controller = loader.getController();
            if (transaction != null) {
                controller.setTransaction(transaction);
            }

            Stage stage = new Stage();
            stage.setTitle(transaction == null ? "Add Transaction" : "Edit Transaction");
            stage.setScene(new Scene(root));

            // This forces the user to interact with the popup before clicking the main window again
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait(); // Pauses code execution here until the popup is closed

            // Automatically refresh the table after the popup closes!
            loadTransactions();
            messageLabel.setText(""); // Clear old messages

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error opening form.");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void handleAdd() {
        openTransactionForm(null); // Passing null tells it to create a NEW transaction
    }

    @FXML
    private void handleEdit() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openTransactionForm(selected); // Passes the selected data to the popup
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
        } else {
            messageLabel.setText("Please select a transaction to delete first.");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }
}