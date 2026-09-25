package com.walletwise;

import com.walletwise.dao.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void init() throws Exception {
        DatabaseInitializer.initialize();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);

        primaryStage.setTitle("WalletWise - Personal Finance Manager");
        primaryStage.setScene(scene);

        // APP CONSTRAINTS: Prevents the user from breaking the UI by shrinking it too much
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(400);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}