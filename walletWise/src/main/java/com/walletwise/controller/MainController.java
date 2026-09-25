package com.walletwise.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.beans.binding.Bindings;

import java.io.IOException;

public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        // Sidebar resizing constraints
        sidebar.prefWidthProperty().bind(
                Bindings.min(250, Bindings.max(150, rootPane.widthProperty().multiply(0.22)))
        );

        // Load the Dashboard automatically when the app starts!
        showDashboard();
    }

    // --- Helper Method to load FXML pages ---
    private void loadPage(String fxmlFileName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFileName));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
            setContent("Error loading page: " + fxmlFileName);
        }
    }

    @FXML
    private void showDashboard() {
        loadPage("DashboardLayout.fxml");
    }

    @FXML
    private void showTransactions() { setContent("Transactions View (Coming in Phase 7)"); }

    @FXML
    private void showBudget() { setContent("Budget View (Coming in Phase 10)"); }

    @FXML
    private void showCurrency() { setContent("Currency Converter (Coming in Phase 13)"); }

    // Fallback method for views we haven't built yet
    private void setContent(String text) {
        contentArea.getChildren().clear();
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 24px; -fx-text-fill: #34495e;");
        contentArea.getChildren().add(label);
    }
}