package com.walletwise;

import com.walletwise.dao.SettingsDAO;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        SettingsDAO settingsDAO = new SettingsDAO();
        String savedPasscode = settingsDAO.getSetting("app_passcode");

        Parent root;
        // Check if a passcode exists in the database
        if (savedPasscode != null && !savedPasscode.isEmpty()) {
            // App is locked, load the Login Screen
            root = FXMLLoader.load(getClass().getResource("/fxml/LoginLayout.fxml"));
            primaryStage.setTitle("WalletWise - Locked");
            primaryStage.setScene(new Scene(root, 500, 400));
        } else {
            // No passcode set, go straight to the Main Dashboard
            root = FXMLLoader.load(getClass().getResource("/fxml/MainLayout.fxml"));
            primaryStage.setTitle("WalletWise - Personal Finance Manager");
            primaryStage.setScene(new Scene(root, 1100, 700));
        }

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}