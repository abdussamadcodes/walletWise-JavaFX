package com.walletwise.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.walletwise.dao.SettingsDAO;
import com.walletwise.dao.TransactionDAO;
import com.walletwise.model.Transaction;
import com.walletwise.util.CategoryUtil;
import com.walletwise.util.CurrencyUtil;
import com.walletwise.util.SecurityUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SettingsController {

    @FXML private CheckBox autoBackupCheck;
    @FXML private PasswordField currentPassField;
    @FXML private PasswordField passField1;
    @FXML private PasswordField passField2;
    @FXML private Label securityStatusLabel;
    private final SettingsDAO settingsDAO = new SettingsDAO();

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

        String backupSetting = settingsDAO.getSetting("auto_backup");
        autoBackupCheck.setSelected("true".equals(backupSetting));

        autoBackupCheck.setOnAction(e -> {
            settingsDAO.saveSetting("auto_backup", autoBackupCheck.isSelected() ? "true" : "false");
        });

        searchField.textProperty().addListener((obs, oldV, newV) -> updateFilteredPreview());
        typeFilterCombo.valueProperty().addListener((obs, oldV, newV) -> updateFilteredPreview());
        categoryFilterCombo.valueProperty().addListener((obs, oldV, newV) -> updateFilteredPreview());
        startDatePicker.valueProperty().addListener((obs, oldV, newV) -> updateFilteredPreview());
        endDatePicker.valueProperty().addListener((obs, oldV, newV) -> updateFilteredPreview());
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
        amountCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%s %.2f", sym, cellData.getValue().getAmount())));
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

            exportPreviewLabel.setText(filteredData.size() + " records selected");
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
    private void handleExportPDF() {
        if (filteredData.isEmpty()) {
            exportStatusLabel.setText("No transactions match the selected filters.");
            exportStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        String code = CurrencyUtil.getCurrencyCode();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Transactions PDF Report");
        // DYNAMIC PDF FILE NAME WITH CURRENCY
        fileChooser.setInitialFileName("walletwise_report_" + LocalDate.now() + "_in_" + code + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));

        File file = fileChooser.showSaveDialog(searchField.getScene().getWindow());
        if (file != null) {
            try {
                Document document = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.BLACK);
                Paragraph title = new Paragraph("WalletWise Financial Report", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);

                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1, 2, 2, 3, 2, 4});

                String[] headers = {"ID", "Date", "Type", "Category", "Amount", "Description"};
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                    cell.setBackgroundColor(new Color(44, 62, 80));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(8);
                    table.addCell(cell);
                }

                double totalIncome = 0;
                double totalExpense = 0;
                Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

                for (Transaction t : filteredData) {
                    PdfPCell idCell = new PdfPCell(new Phrase(String.valueOf(t.getId()), dataFont));
                    idCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(idCell);

                    table.addCell(new Phrase(t.getDate() != null ? dateFormatter.format(t.getDate()) : "", dataFont));
                    table.addCell(new Phrase(t.getType(), dataFont));
                    table.addCell(new Phrase(t.getCategoryName(), dataFont));
                    table.addCell(new Phrase(String.format("%s %.2f", code, t.getAmount()), dataFont));
                    table.addCell(new Phrase(t.getDescription() != null ? t.getDescription() : "", dataFont));

                    if ("Income".equalsIgnoreCase(t.getType())) {
                        totalIncome += t.getAmount();
                    } else if ("Expense".equalsIgnoreCase(t.getType())) {
                        totalExpense += t.getAmount();
                    }
                }
                document.add(table);

                double balance = totalIncome - totalExpense;

                Font summaryLabelFont = FontFactory.getFont(FontFactory.HELVETICA, 14, Color.DARK_GRAY);
                Font summaryValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);

                Paragraph summary = new Paragraph();
                summary.setSpacingBefore(20f);

                summary.add(new Phrase("Total Income: ", summaryLabelFont));
                summary.add(new Phrase(String.format("%s %.2f\n", code, totalIncome), summaryValueFont));

                summary.add(new Phrase("Total Expense: ", summaryLabelFont));
                summary.add(new Phrase(String.format("%s %.2f\n", code, totalExpense), summaryValueFont));

                summary.add(new Phrase("Current Balance: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK)));

                Color balanceColor = balance >= 0 ? new Color(39, 174, 96) : new Color(231, 76, 60);
                Font balanceFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, balanceColor);
                summary.add(new Phrase(String.format("%s %.2f", code, balance), balanceFont));

                document.add(summary);
                document.close();

                exportStatusLabel.setText("Successfully exported PDF Report!");
                exportStatusLabel.setStyle("-fx-text-fill: #8e44ad;");

            } catch (Exception e) {
                exportStatusLabel.setText("PDF Export failed: " + e.getMessage());
                exportStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleExportJSON() {
        if (filteredData.isEmpty()) {
            exportStatusLabel.setText("No transactions match the selected filters.");
            exportStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        String code = CurrencyUtil.getCurrencyCode();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Transactions JSON");
        // DYNAMIC JSON FILE NAME WITH CURRENCY
        fileChooser.setInitialFileName("walletwise_export_" + LocalDate.now() + "_in_" + code + ".json");
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
                                code
                        )).collect(Collectors.toList());

                mapper.writeValue(file, exportList);
                exportStatusLabel.setText("Successfully exported JSON!");
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

                String systemCurrency = CurrencyUtil.getCurrencyCode();
                boolean currencyMismatch = importedList.stream().anyMatch(dto ->
                        dto.getCurrency() != null && !dto.getCurrency().isEmpty() && !dto.getCurrency().equalsIgnoreCase(systemCurrency)
                );

                if (currencyMismatch) {
                    showPremiumCurrencyWarning(systemCurrency);
                    importStatusLabel.setText("Import cancelled: Currency mismatch.");
                    importStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    return;
                }

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
                                parsedDate, systemCurrency
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
                importStatusLabel.setText("Import Error: Invalid or corrupted file.");
                importStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        }
    }

    private void showPremiumCurrencyWarning(String systemCurrency) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Import Failed");

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.OK);

        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #e74c3c; -fx-border-width: 4 0 0 0;");

        VBox content = new VBox();
        content.setSpacing(12);
        content.setPadding(new Insets(20));
        content.setPrefWidth(420);

        Label warningHeader = new Label("Currency Mismatch Detected");
        warningHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #c0392b;");

        Label warningText = new Label(
                "Your WalletWise is currently configured for " + systemCurrency + ".\n\n" +
                        "The JSON file you are attempting to import contains transactions recorded in a different currency. " +
                        "To prevent inaccurate calculations and corrupted charts, this import has been blocked.\n\n" +
                        "Please select a file matching your preferred currency."
        );
        warningText.setWrapText(true);
        warningText.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        warningText.setStyle("-fx-text-fill: #34495e; -fx-font-size: 14px; -fx-line-spacing: 4px;");

        content.getChildren().addAll(warningHeader, warningText);
        dialogPane.setContent(content);

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20; -fx-background-radius: 6;");
        }

        alert.showAndWait();
    }

    @FXML
    private void handleSavePasscode() {
        String currentPass = currentPassField.getText();
        String p1 = passField1.getText();
        String p2 = passField2.getText();

        String savedHash = settingsDAO.getSetting("app_passcode");

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

        if (p1 == null || p1.isEmpty()) {
            securityStatusLabel.setText("New passcode cannot be empty.");
            securityStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        String validationErrors = validatePasswordConstraints(p1);
        if (validationErrors != null) {
            securityStatusLabel.setText(validationErrors);
            securityStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        if (!p1.equals(p2)) {
            securityStatusLabel.setText("New passcodes do not match!");
            securityStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

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

    private String validatePasswordConstraints(String password) {
        StringBuilder errors = new StringBuilder();

        if (password.length() < 8) errors.append("- At least 8 characters long\n");
        if (!password.matches(".*[a-zA-Z].*")) errors.append("- At least one letter\n");
        if (!password.matches(".*\\d.*")) errors.append("- At least one digit (0-9)\n");
        if (!password.matches(".*[^a-zA-Z0-9].*")) errors.append("- At least one special character\n");

        if (errors.length() > 0) return "Passcode missing constraints:\n" + errors.toString().trim();
        return null;
    }

    @FXML
    private void handleFactoryReset() {
        String savedHash = settingsDAO.getSetting("app_passcode");

        if (savedHash != null && !savedHash.isEmpty()) {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Authentication Required");

            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #2980b9; -fx-border-width: 4 0 0 0;");

            VBox content = new VBox();
            content.setSpacing(12);
            content.setPadding(new Insets(20));
            content.setPrefWidth(400);

            Label header = new Label("App is locked.");
            header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            Label instructions = new Label("Enter your current passcode to authorize the factory reset.");
            instructions.setWrapText(true);
            instructions.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            instructions.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");

            PasswordField pwd = new PasswordField();
            pwd.setPromptText("Current Passcode");
            pwd.setStyle("-fx-font-size: 14px; -fx-padding: 8; -fx-background-radius: 5;");

            content.getChildren().addAll(header, instructions, pwd);
            dialogPane.setContent(content);

            ButtonType confirmButtonType = new ButtonType("Authorize", ButtonBar.ButtonData.OK_DONE);
            dialogPane.getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

            Button confirmBtn = (Button) dialogPane.lookupButton(confirmButtonType);
            confirmBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20; -fx-background-radius: 6;");

            Button cancelBtn = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
            cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #95a5a6; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20; -fx-border-color: #bdc3c7; -fx-border-radius: 6;");

            Platform.runLater(pwd::requestFocus);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == confirmButtonType) return pwd.getText();
                return null;
            });

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String inputHash = SecurityUtil.hashPassword(result.get());
                if (!inputHash.equals(savedHash)) {
                    securityStatusLabel.setText("Incorrect passcode. Factory reset aborted.");
                    securityStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    return;
                }
            } else {
                return;
            }
        }

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Factory Reset");

        DialogPane alertPane = alert.getDialogPane();
        alertPane.setStyle("-fx-background-color: white; -fx-border-color: #c0392b; -fx-border-width: 4 0 0 0;");

        VBox alertContent = new VBox();
        alertContent.setSpacing(12);
        alertContent.setPadding(new Insets(20));
        alertContent.setPrefWidth(420);

        Label warningHeader = new Label("Permanent Data Deletion");
        warningHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #c0392b;");

        Label warningText = new Label("Are you absolutely sure you want to reset WalletWise?\n\nThis will permanently delete all transactions, budgets, categories, and settings. This action cannot be undone.");
        warningText.setWrapText(true);
        warningText.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        warningText.setStyle("-fx-text-fill: #34495e; -fx-font-size: 14px; -fx-line-spacing: 4px;");

        alertContent.getChildren().addAll(warningHeader, warningText);
        alertPane.setContent(alertContent);

        alertPane.getButtonTypes().clear();
        ButtonType resetBtnType = new ButtonType("Reset Everything", ButtonBar.ButtonData.OK_DONE);
        alertPane.getButtonTypes().addAll(resetBtnType, ButtonType.CANCEL);

        Button resetBtn = (Button) alertPane.lookupButton(resetBtnType);
        resetBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20; -fx-background-radius: 6;");

        Button alertCancelBtn = (Button) alertPane.lookupButton(ButtonType.CANCEL);
        alertCancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #95a5a6; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20; -fx-border-color: #bdc3c7; -fx-border-radius: 6;");

        Optional<ButtonType> confirmation = alert.showAndWait();
        if (confirmation.isPresent() && confirmation.get() == resetBtnType) {
            performFullReset();
        }
    }

    private void performFullReset() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:walletwise.db");
             Statement stmt = conn.createStatement()) {

            stmt.execute("DELETE FROM transactions");
            stmt.execute("DELETE FROM categories");
            stmt.execute("DELETE FROM budget_limits");
            stmt.execute("DELETE FROM app_settings");
            stmt.execute("DELETE FROM sqlite_sequence");

            stmt.execute("INSERT INTO categories (name, type) VALUES " +
                    "('Salary', 'Income'), " +
                    "('Freelance', 'Income'), " +
                    "('Food', 'Expense'), " +
                    "('Transport', 'Expense'), " +
                    "('Utilities', 'Expense');");

            Platform.runLater(() -> {
                try {
                    javafx.stage.Stage stage = (javafx.stage.Stage) securityStatusLabel.getScene().getWindow();
                    javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/WelcomeGuideLayout.fxml"));
                    stage.setTitle("Welcome to WalletWise");

                    stage.setScene(new javafx.scene.Scene(root, 650, 550));
                    stage.setMinWidth(600);
                    stage.setMinHeight(500);

                    stage.setMaximized(false);
                    stage.setResizable(true);
                    stage.centerOnScreen();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {
            securityStatusLabel.setText("Reset failed: " + e.getMessage());
            securityStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
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