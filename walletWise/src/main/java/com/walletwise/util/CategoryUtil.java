package com.walletwise.util;

import com.walletwise.dao.CategoryDAO;
import com.walletwise.dao.TransactionDAO;
import com.walletwise.model.Category;
import com.walletwise.model.Transaction;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CategoryUtil {
    private static final CategoryDAO categoryDAO = new CategoryDAO();
    private static final TransactionDAO transactionDAO = new TransactionDAO();

    // Fetches ALL unique categories (Used for Transactions and Settings)
    public static List<String> getAllUniqueCategories(boolean includeAllOption) {
        List<String> list = new ArrayList<>();
        if (includeAllOption) list.add("All Categories");

        for (Category c : categoryDAO.getAll()) addUnique(list, c.getName());
        for (Transaction t : transactionDAO.getAll()) addUnique(list, t.getCategoryName());

        return list;
    }

    // Fetches ONLY Expense categories (Used for the Budget Planner)
    public static List<String> getExpenseCategories() {
        List<String> list = new ArrayList<>();
        for (Category c : categoryDAO.getAll()) {
            if ("Expense".equalsIgnoreCase(c.getType())) addUnique(list, c.getName());
        }
        for (Transaction t : transactionDAO.getAll()) {
            if ("Expense".equalsIgnoreCase(t.getType())) addUnique(list, t.getCategoryName());
        }
        return list;
    }

    // Prevents duplicates and fixes casing (e.g., "food" becomes "Food")
    private static void addUnique(List<String> list, String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) return;
        String cleanName = rawName.substring(0, 1).toUpperCase() + rawName.substring(1).toLowerCase().trim();
        for (String existing : list) {
            if (existing.equalsIgnoreCase(cleanName)) return;
        }
        list.add(cleanName);
    }

    // Turns a standard ComboBox into a professional, stable auto-complete field
    public static void makeAutoComplete(ComboBox<String> comboBox, List<String> items) {
        ObservableList<String> originalItems = FXCollections.observableArrayList(items);
        comboBox.setItems(originalItems);
        comboBox.setEditable(true);

        TextField editor = comboBox.getEditor();

        editor.setOnKeyReleased(event -> {
            // Ignore navigation keys so the user can still use arrows/enter to select items
            switch (event.getCode()) {
                case UP: case DOWN: case LEFT: case RIGHT:
                case HOME: case END: case TAB: case ENTER: case ESCAPE:
                    return;
                default:
                    break;
            }

            String typedText = editor.getText();
            if (typedText == null) typedText = "";

            final String finalText = typedText;

            Platform.runLater(() -> {
                if (finalText.isEmpty()) {
                    comboBox.setValue(null);
                    comboBox.setItems(originalItems);
                    editor.setText("");
                    comboBox.hide();
                } else {
                    String filter = finalText.toLowerCase();
                    List<String> filteredList = originalItems.stream()
                            .filter(item -> item.toLowerCase().contains(filter))
                            .collect(Collectors.toList());


                    comboBox.setItems(FXCollections.observableArrayList(filteredList));
                    editor.setText(finalText);
                    editor.positionCaret(finalText.length());

                    if (!filteredList.isEmpty()) {
                        comboBox.show();
                    } else {
                        comboBox.hide();
                    }
                }
            });
        });

        editor.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String currentText = editor.getText();
                if (currentText == null) currentText = "";

                comboBox.hide();
                comboBox.setItems(originalItems);


                comboBox.setValue(currentText);
                editor.setText(currentText);
            }
        });
    }
}