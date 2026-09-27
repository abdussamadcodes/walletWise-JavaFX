package com.walletwise.controller;

import com.walletwise.dao.SettingsDAO;
import com.walletwise.util.CurrencyUtil;
import com.walletwise.util.SecurityUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class WelcomeGuideController {
    @FXML private ComboBox<String> currencyCombo;
    @FXML private PasswordField passField1;
    @FXML private PasswordField passField2;
    @FXML private CheckBox autoBackupCheck;
    @FXML private Label errorLabel;

    private final SettingsDAO settingsDAO = new SettingsDAO();

    @FXML
    public void initialize() {
        currencyCombo.setItems(FXCollections.observableArrayList(
                "BDT ৳", "USD $", "EUR €", "GBP £", "INR ₹", "CAD $", "AUD $"
        ));
        currencyCombo.setValue("BDT ৳");
        autoBackupCheck.setSelected(true);
    }

    @FXML
    private void handleSkip() {
        completeSetup();
    }

    @FXML
    private void handleFinish() {
        String selected = currencyCombo.getValue();
        if (selected != null) {
            CurrencyUtil.saveCurrency(selected.substring(0, 3));
        }

        settingsDAO.saveSetting("auto_backup", autoBackupCheck.isSelected() ? "true" : "false");

        String p1 = passField1.getText();
        String p2 = passField2.getText();

        if (p1 != null && !p1.isEmpty()) {
            if (p1.length() < 8 || !p1.matches(".*[a-zA-Z].*") || !p1.matches(".*\\d.*") || !p1.matches(".*[^a-zA-Z0-9].*")) {
                errorLabel.setText("Passcode must be 8+ chars, with letter, number, and special char.");
                return;
            }
            if (!p1.equals(p2)) {
                errorLabel.setText("Passcodes do not match!");
                return;
            }
            settingsDAO.saveSetting("app_passcode", SecurityUtil.hashPassword(p1));
        }
        completeSetup();
    }

    private void completeSetup() {
        settingsDAO.saveSetting("setup_complete", "true");

        if (settingsDAO.getSetting("app_currency_code") == null) {
            CurrencyUtil.saveCurrency("BDT");
        }

        try {
            Stage stage = (Stage) currencyCombo.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainLayout.fxml"));
            stage.setTitle("WalletWise - Personal Finance Manager");
            stage.setScene(new Scene(root, 950, 600));

            // Apply lowest reasonable constraints for the main app
            stage.setMinWidth(800);
            stage.setMinHeight(500);

            // Maximize automatically
            stage.setMaximized(true);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}