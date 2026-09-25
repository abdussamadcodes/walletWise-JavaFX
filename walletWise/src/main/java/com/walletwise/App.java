package com.walletwise;

import com.walletwise.dao.DatabaseInitializer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void init() throws Exception {
        // The init() method runs before the UI starts. Perfect for DB setup.
        DatabaseInitializer.initialize();
    }

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Phase 2: Database Connected!");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("WalletWise - Personal Finance Manager");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}