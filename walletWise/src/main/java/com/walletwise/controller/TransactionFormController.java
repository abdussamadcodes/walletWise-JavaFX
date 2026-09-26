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

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList("Income", "Expense"));

        List<Category> categories = categoryDAO.getAll();
        List<String> categoryNames = new ArrayList<>();

        for (Category c : categories) {
            // Clean the name as it comes out of the database
            String cleanName = formatCategoryName(c.getName());

            // Check if we already added a matching name (ignoring uppercase/lowercase)
            boolean alreadyAdded = false;
            for (String existing : categoryNames) {
                if (existing.equalsIgnoreCase(cleanName)) {
                    alreadyAdded = true;
                    break;
                }
            }

            // Only add it to the dropdown if it's not a duplicate
            if (!alreadyAdded) {
                categoryNames.add(cleanName);
            }
        }

        categoryCombo.setItems(FXCollections.observableArrayList(categoryNames));

        makeDatePickerTypable(datePicker);
        datePicker.setValue(LocalDate.now());
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
                    try {
                        return LocalDate.parse(string.trim(), dateFormatter);
                    } catch (Exception e) {
                        return picker.getValue();
                    }
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

    public void setTransaction(Transaction t) {
        this.transactionToEdit = t;
        titleLabel.setText("Edit Transaction");

        amountField.setText(String.valueOf(t.getAmount()));
        typeCombo.setValue(t.getType());
        datePicker.setValue(t.getDate());
        descField.setText(t.getDescription());
        categoryCombo.setValue(t.getCategoryName());
    }

    // --- NEW HELPER: Formats "food" into "Food" ---
    private String formatCategoryName(String rawName) {
        if (rawName == null || rawName.isEmpty()) return rawName;
        // Capitalize first letter, lowercase the rest
        return rawName.substring(0, 1).toUpperCase() + rawName.substring(1).toLowerCase();
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

            // FIX: Apply the formatting to the category name right here
            String categoryName = formatCategoryName(categoryCombo.getValue().trim());

            LocalDate date = datePicker.getValue();
            String desc = descField.getText();

            // Ignore case when checking if category already exists
            boolean categoryExists = false;
            for (String item : categoryCombo.getItems()) {
                if (item.equalsIgnoreCase(categoryName)) {
                    categoryExists = true;
                    break;
                }
            }

            if (!categoryExists) {
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