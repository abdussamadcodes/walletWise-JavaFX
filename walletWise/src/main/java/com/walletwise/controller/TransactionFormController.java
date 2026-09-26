package com.walletwise.controller;

import com.walletwise.dao.CategoryDAO;
import com.walletwise.dao.TransactionDAO;
import com.walletwise.model.Category;
import com.walletwise.model.Transaction;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransactionFormController {

    @FXML private Label titleLabel;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField descField;
    @FXML private Label errorLabel;

    private Transaction transactionToEdit = null;
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    // Define the custom date format (Day-Month-Year)
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList("Income", "Expense"));

        List<Category> categories = categoryDAO.getAll();
        List<String> categoryNames = new ArrayList<>();
        for (Category c : categories) {
            if (!categoryNames.contains(c.getName())) {
                categoryNames.add(c.getName());
            }
        }
        categoryCombo.setItems(FXCollections.observableArrayList(categoryNames));

        // FIX: Force the DatePicker to use DD-MM-YYYY format
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? dateFormatter.format(date) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                return (string != null && !string.isEmpty()) ? LocalDate.parse(string, dateFormatter) : null;
            }
        });

        datePicker.setValue(LocalDate.now());
    }

    public void setTransaction(Transaction t) {
        this.transactionToEdit = t;
        titleLabel.setText("Edit Transaction");

        amountField.setText(String.valueOf(t.getAmount()));
        typeCombo.setValue(t.getType());
        datePicker.setValue(t.getDate());
        descField.setText(t.getDescription());
        categoryCombo.setValue(t.getCategoryName());
    }

    @FXML
    private void handleSave() {
        try {
            if (amountField.getText().isEmpty() || typeCombo.getValue() == null
                    || categoryCombo.getValue() == null || categoryCombo.getValue().trim().isEmpty()
                    || datePicker.getValue() == null) {
                errorLabel.setText("Please fill all required fields.");
                return;
            }

            double amount = Double.parseDouble(amountField.getText());
            String type = typeCombo.getValue();
            String categoryName = categoryCombo.getValue().trim();
            LocalDate date = datePicker.getValue();
            String desc = descField.getText();

            if (!categoryCombo.getItems().contains(categoryName)) {
                categoryDAO.add(new Category(categoryName, type));
            }

            if (transactionToEdit == null) {
                Transaction newTx = new Transaction(amount, type, categoryName, desc, date, "USD");
                transactionDAO.add(newTx);
            } else {
                transactionToEdit.setAmount(amount);
                transactionToEdit.setType(type);
                transactionToEdit.setCategoryName(categoryName);
                transactionToEdit.setDate(date);
                transactionToEdit.setDescription(desc);
                transactionDAO.update(transactionToEdit);
            }

            closeWindow();

        } catch (NumberFormatException e) {
            errorLabel.setText("Amount must be a valid number (e.g. 15.50).");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}