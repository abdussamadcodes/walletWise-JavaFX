package com.walletwise.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.beans.binding.Bindings;

public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        sidebar.prefWidthProperty().bind(
                Bindings.min(250, Bindings.max(150, rootPane.widthProperty().multiply(0.22)))
        );
        showDashboard();
    }

    // --- UPDATED METHOD TO SHOW EXACT ERRORS ---
    private void loadPage(String fxmlFileName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFileName));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();

            // Get the deepest cause of the error
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            // Show the exact error on the screen!
            setContent("Crash Reason:\n" + cause.toString());
        }
    }

    @FXML
    private void showDashboard() {
        loadPage("DashboardLayout.fxml");
    }

    @FXML
    private void showTransactions() {
        loadPage("TransactionsLayout.fxml");
    }
    @FXML
    private void showBudget() {
        loadPage("BudgetLayout.fxml");
    }
    @FXML
    private void showCurrency() {
        loadPage("CurrencyLayout.fxml");
    }
    @FXML
    private void showSettings() {
        loadPage("SettingsLayout.fxml");
    }
    private void setContent(String text) {
        contentArea.getChildren().clear();
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 18px; -fx-text-fill: #e74c3c; -fx-padding: 20; -fx-wrap-text: true;");
        contentArea.getChildren().add(label);
    }
}