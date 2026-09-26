package com.walletwise.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.walletwise.dao.SettingsDAO;
import com.walletwise.dao.TransactionDAO;
import com.walletwise.model.Transaction;
import com.walletwise.util.CategoryUtil;
import com.walletwise.util.SecurityUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SettingsController {

    // --- SECURITY FIELDS ---
    @FXML private PasswordField currentPassField;
    @FXML private PasswordField passField1;
    @FXML private PasswordField passField2;
    @FXML private Label securityStatusLabel;
    private final SettingsDAO settingsDAO = new SettingsDAO();

    // --- EXPORT/IMPORT FIELDS ---
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label exportPreviewLabel;
    @FXML private Label exportStatusLabel;
    @FXML private Label importStatusLabel;

    @FXML private TableView<Transaction> previewTable;
    @FXML private TableColumn<Transaction, Integer> idCol;
    @FXML private TableColumn<Transaction, String> dateCol;
    @FXML private TableColumn<Transaction, String> typeCol;
    @FXML private TableColumn<Transaction, String> categoryCol;
    @FXML private TableColumn<Transaction, String> amountCol;
    @FXML private TableColumn<Transaction, String> descCol;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private ObservableList<Transaction> allTransactions;
    private FilteredList<Transaction> filteredData;

    @FXML
    public void initialize() {
        makeDatePickerTypable(startDatePicker);
        makeDatePickerTypable(endDatePicker);

        setupTableColumns();

        typeFilterCombo.setItems(FXCollections.observableArrayList("All Types", "Income", "Expense"));
        typeFilterCombo.setValue("All Types");

        loadCategories();

        allTransactions = FXCollections.observableArrayList(transactionDAO.getAll());
        filteredData = new FilteredList<>(allTransactions, p -> true);

        SortedList<Transaction> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(previewTable.comparatorProperty());
        previewTable.setItems(sortedData);

        updateFilteredPreview();

        searchField.textProperty().addListener((obs, oldV, newV) -> updateFilteredPreview());
        typeFilterCombo.valueProperty().addListener((obs, oldV, newV) -> updateFilteredPreview());
        categoryFilterCombo.valueProperty().addListener((obs, oldV, newV) -> updateFilteredPreview());
        startDatePicker.valueProperty().addListener((obs, oldV, newV) -> updateFilteredPreview());
        endDatePicker.valueProperty().addListener((obs, oldV, newV) -> updateFilteredPreview());
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

    private void loadCategories() {
        List<String> categories = CategoryUtil.getAllUniqueCategories(true);
        CategoryUtil.makeAutoComplete(categoryFilterCombo, categories);
        categoryFilterCombo.setValue("All Categories");
    }

    private void refreshTransactionsData() {
        allTransactions.setAll(transactionDAO.getAll());
        updateFilteredPreview();
    }

    private void updateFilteredPreview() {
        Platform.runLater(() -> {
            String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            String selectedType = typeFilterCombo.getValue();
            String selectedCategory = categoryFilterCombo.getValue() == null ? "" : categoryFilterCombo.getValue().trim();
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            filteredData.setPredicate(t -> {
                if (selectedType != null && !selectedType.equals("All Types") && !t.getType().equalsIgnoreCase(selectedType)) return false;

                if (!selectedCategory.isEmpty() && !selectedCategory.equalsIgnoreCase("All Categories")) {
                    if (!t.getCategoryName().equalsIgnoreCase(selectedCategory)) return false;
                }

                LocalDate d = t.getDate();
                if (start != null && d != null && d.isBefore(start)) return false;
                if (end != null && d != null && d.isAfter(end)) return false;

                if (query.isEmpty()) return true;
                if (String.valueOf(t.getId()).equals(query)) return true;
                return t.getDescription() != null && t.getDescription().toLowerCase().contains(query);
            });

            exportPreviewLabel.setText(filteredData.size() + " records selected for export");
        });
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        typeFilterCombo.setValue("All Types");
        categoryFilterCombo.setValue("All Categories");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        exportStatusLabel.setText("");
    }

    @FXML
    private void handleExportJSON() {
        if (filteredData.isEmpty()) {
            exportStatusLabel.setText("No transactions match the selected filters.");
            exportStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Transactions JSON");
        fileChooser.setInitialFileName("walletwise_export_" + LocalDate.now() + ".json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = fileChooser.showSaveDialog(searchField.getScene().getWindow());
        if (file != null) {
            try {
                List<TransactionExportDTO> exportList = filteredData.stream()
                        .map(t -> new TransactionExportDTO(
                                t.getAmount(),
                                t.getType(),
                                t.getCategoryName(),
                                t.getDescription(),
                                t.getDate() != null ? dateFormatter.format(t.getDate()) : "",
                                "BDT"
                        )).collect(Collectors.toList());

                mapper.writeValue(file, exportList);
                exportStatusLabel.setText("Successfully exported " + exportList.size() + " records!");
                exportStatusLabel.setStyle("-fx-text-fill: #27ae60;");
            } catch (Exception e) {
                exportStatusLabel.setText("Export failed: " + e.getMessage());
                exportStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        }
    }

    @FXML
    private void handleImportJSON() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select JSON File to Import");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = fileChooser.showOpenDialog(searchField.getScene().getWindow());
        if (file != null) {
            try {
                List<TransactionExportDTO> importedList = mapper.readValue(file, new TypeReference<List<TransactionExportDTO>>() {});
                List<Transaction> currentList = transactionDAO.getAll();

                int importedCount = 0;
                int duplicateCount = 0;

                for (TransactionExportDTO dto : importedList) {
                    if (dto.getAmount() <= 0 || dto.getType() == null || dto.getCategoryName() == null || dto.getDate() == null) continue;

                    LocalDate parsedDate;
                    try { parsedDate = LocalDate.parse(dto.getDate(), dateFormatter); }
                    catch (Exception ex) { continue; }

                    String category = dto.getCategoryName().substring(0, 1).toUpperCase() +
                            dto.getCategoryName().substring(1).toLowerCase().trim();

                    boolean isDuplicate = currentList.stream().anyMatch(existing ->
                            Double.compare(existing.getAmount(), dto.getAmount()) == 0 &&
                                    existing.getType().equalsIgnoreCase(dto.getType()) &&
                                    existing.getCategoryName().equalsIgnoreCase(category) &&
                                    existing.getDate().equals(parsedDate) &&
                                    Objects.equals(
                                            existing.getDescription() == null ? "" : existing.getDescription().trim(),
                                            dto.getDescription() == null ? "" : dto.getDescription().trim()
                                    )
                    );

                    if (isDuplicate) {
                        duplicateCount++;
                    } else {
                        Transaction newTx = new Transaction(
                                dto.getAmount(), dto.getType(), category,
                                dto.getDescription() != null ? dto.getDescription().trim() : "",
                                parsedDate, dto.getCurrency() != null ? dto.getCurrency() : "BDT"
                        );
                        transactionDAO.add(newTx);
                        currentList.add(newTx);
                        importedCount++;
                    }
                }

                refreshTransactionsData();
                loadCategories();

                importStatusLabel.setText(String.format("Import Complete: %d added, %d duplicates skipped.", importedCount, duplicateCount));
                importStatusLabel.setStyle(importedCount > 0 ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #f39c12;");

            } catch (Exception e) {
                importStatusLabel.setText("Import Error: " + e.getMessage());
                importStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        }
    }

    // --- SECURITY METHODS ---
    @FXML
    private void handleSavePasscode() {
        String currentPass = currentPassField.getText();
        String p1 = passField1.getText();
        String p2 = passField2.getText();

        String savedHash = settingsDAO.getSetting("app_passcode");

        // 1. Verify current passcode if one already exists
        if (savedHash != null && !savedHash.isEmpty()) {
            if (currentPass == null || currentPass.isEmpty()) {
                securityStatusLabel.setText("Please enter your current passcode to change it.");
                securityStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                return;
            }
            String currentHash = SecurityUtil.hashPassword(currentPass);
            if (!currentHash.equals(savedHash)) {
                securityStatusLabel.setText("Current passcode is incorrect.");
                securityStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                return;
            }
        }

        // 2. Validate new passcode is not empty
        if (p1 == null || p1.isEmpty()) {
            securityStatusLabel.setText("New passcode cannot be empty.");
            securityStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        // 3. Check password constraints
        String validationErrors = validatePasswordConstraints(p1);
        if (validationErrors != null) {
            securityStatusLabel.setText(validationErrors);
            securityStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        // 4. Confirm new passcodes match
        if (!p1.equals(p2)) {
            securityStatusLabel.setText("New passcodes do not match!");
            securityStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        // 5. Save the new passcode
        String hashedPassword = SecurityUtil.hashPassword(p1);
        settingsDAO.saveSetting("app_passcode", hashedPassword);

        currentPassField.clear();
        passField1.clear();
        passField2.clear();
        securityStatusLabel.setText("Passcode saved successfully! App is now locked.");
        securityStatusLabel.setStyle("-fx-text-fill: #27ae60;");
    }

    @FXML
    private void handleRemovePasscode() {
        String currentPass = currentPassField.getText();
        String savedHash = settingsDAO.getSetting("app_passcode");

        if (savedHash == null || savedHash.isEmpty()) {
            securityStatusLabel.setText("No passcode is currently set.");
            securityStatusLabel.setStyle("-fx-text-fill: #f39c12;");
            return;
        }

        if (currentPass == null || currentPass.isEmpty()) {
            securityStatusLabel.setText("Please enter your current passcode to remove it.");
            securityStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        String currentHash = SecurityUtil.hashPassword(currentPass);
        if (!currentHash.equals(savedHash)) {
            securityStatusLabel.setText("Current passcode is incorrect.");
            securityStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        settingsDAO.deleteSetting("app_passcode");
        currentPassField.clear();
        passField1.clear();
        passField2.clear();
        securityStatusLabel.setText("Passcode removed. App is now unlocked.");
        securityStatusLabel.setStyle("-fx-text-fill: #f39c12;");
    }

    // --- HELPER METHOD: Validates strict password constraints ---
    private String validatePasswordConstraints(String password) {
        StringBuilder errors = new StringBuilder();

        if (password.length() < 8) {
            errors.append("- At least 8 characters long\n");
        }
        if (!password.matches(".*[a-zA-Z].*")) {
            errors.append("- At least one letter\n");
        }
        if (!password.matches(".*\\d.*")) {
            errors.append("- At least one digit (0-9)\n");
        }
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            errors.append("- At least one special character\n");
        }

        if (errors.length() > 0) {
            return "Passcode missing constraints:\n" + errors.toString().trim();
        }
        return null;
    }

    public static class TransactionExportDTO {
        private double amount; private String type; private String categoryName;
        private String description; private String date; private String currency;

        public TransactionExportDTO() {}
        public TransactionExportDTO(double amount, String type, String categoryName, String description, String date, String currency) {
            this.amount = amount; this.type = type; this.categoryName = categoryName;
            this.description = description; this.date = date; this.currency = currency;
        }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }
}