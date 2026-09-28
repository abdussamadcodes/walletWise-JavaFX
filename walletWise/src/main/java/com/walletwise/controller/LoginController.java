package com.walletwise.controller;

import com.walletwise.dao.SettingsDAO;
import com.walletwise.util.SecurityUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private PasswordField passcodeField;
    @FXML private Label errorLabel;

    private final SettingsDAO settingsDAO = new SettingsDAO();

    @FXML
    private void handleLogin() {
        String input = passcodeField.getText();
        String savedHash = settingsDAO.getSetting("app_passcode");

        if (input == null || input.isEmpty()) {
            errorLabel.setText("Please enter a passcode.");
            return;
        }

        String inputHash = SecurityUtil.hashPassword(input);

        if (inputHash.equals(savedHash)) {
            try {
                Stage stage = (Stage) passcodeField.getScene().getWindow();
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainLayout.fxml"));

                stage.setTitle("WalletWise - Personal Finance Manager");

                stage.setScene(new Scene(root, 950, 600));

                stage.setResizable(true);

                stage.setMinWidth(850);
                stage.setMinHeight(550);

                stage.setMaximized(true);

                stage.centerOnScreen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            errorLabel.setText("Incorrect passcode. Try again.");
            passcodeField.clear();
        }
    }
}