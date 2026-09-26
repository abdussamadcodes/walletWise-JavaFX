package com.walletwise;

import com.walletwise.dao.SettingsDAO;
import com.walletwise.util.BackupUtil;
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
        if (savedPasscode != null && !savedPasscode.isEmpty()) {
            root = FXMLLoader.load(getClass().getResource("/fxml/LoginLayout.fxml"));
            primaryStage.setTitle("WalletWise - Locked");
            primaryStage.setScene(new Scene(root, 500, 400));
            primaryStage.setResizable(false);
        } else {
            root = FXMLLoader.load(getClass().getResource("/fxml/MainLayout.fxml"));
            primaryStage.setTitle("WalletWise - Personal Finance Manager");

            primaryStage.setScene(new Scene(root, 950, 600));
            primaryStage.setMinWidth(850);
            primaryStage.setMinHeight(550);
            primaryStage.setMaximized(true);
        }

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        SettingsDAO settingsDAO = new SettingsDAO();
        String autoBackup = settingsDAO.getSetting("auto_backup");

        if ("true".equals(autoBackup)) {
            System.out.println("Auto-backup is enabled. Backing up database...");
            BackupUtil.createBackup();
        }

        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}