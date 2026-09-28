package com.walletwise;

import com.walletwise.dao.DatabaseInitializer;
import com.walletwise.dao.SettingsDAO;
import com.walletwise.util.BackupUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void init() throws Exception {
        DatabaseInitializer.initialize();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SettingsDAO settingsDAO = new SettingsDAO();
        String setupComplete = settingsDAO.getSetting("setup_complete");
        String savedPasscode = settingsDAO.getSetting("app_passcode");

        Parent root;
        if (setupComplete == null || !setupComplete.equals("true")) {
            root = FXMLLoader.load(getClass().getResource("/fxml/WelcomeGuideLayout.fxml"));
            primaryStage.setTitle("Welcome to WalletWise");


            primaryStage.setScene(new Scene(root, 650, 550));
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(500);
            primaryStage.setResizable(true);

        } else if (savedPasscode != null && !savedPasscode.isEmpty()) {
            root = FXMLLoader.load(getClass().getResource("/fxml/LoginLayout.fxml"));
            primaryStage.setTitle("WalletWise - Locked");
            primaryStage.setScene(new Scene(root, 500, 400));
            primaryStage.setResizable(false);

        } else {
            root = FXMLLoader.load(getClass().getResource("/fxml/MainLayout.fxml"));
            primaryStage.setTitle("WalletWise - Personal Finance Manager");
            primaryStage.setScene(new Scene(root, 950, 600));


            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(500);
            primaryStage.setMaximized(true);
        }

        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        SettingsDAO settingsDAO = new SettingsDAO();
        if ("true".equals(settingsDAO.getSetting("auto_backup"))) {
            System.out.println("Auto-backup is enabled. Backing up database...");
            BackupUtil.createBackup();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}