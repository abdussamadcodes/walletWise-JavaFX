package com.walletwise.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CurrencyController {

    @FXML private TextField amountField;
    @FXML private ComboBox<String> fromCurrencyCombo;
    @FXML private ComboBox<String> toCurrencyCombo;
    @FXML private Label resultLabel;
    @FXML private Label errorLabel;
    @FXML private Label rateInfoLabel;

    private Map<String, Double> exchangeRates = new HashMap<>();

    private final String CACHE_FILE = "exchange_rates_cache.json";
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        rateInfoLabel.setText("Checking internet for latest rates...");
        rateInfoLabel.setStyle("-fx-text-fill: #f39c12;"); // Orange

        // Run the 3-step fallback logic on a background thread
        CompletableFuture.runAsync(this::loadRatesWithFallback);
    }

    // =========================================================
    // THE 3-STEP FALLBACK LOGIC
    // =========================================================
    private void loadRatesWithFallback() {
        try {
            // STEP 1: Check Internet First
            fetchLiveRates();

            Platform.runLater(() -> {
                updateDropdowns();
                rateInfoLabel.setText("Live rates connected and saved!");
                rateInfoLabel.setStyle("-fx-text-fill: #27ae60;"); // Green
            });

        } catch (Exception e1) {
            try {
                // STEP 2: If Internet fails, check Hard Drive Cache
                loadCachedRates();

                Platform.runLater(() -> {
                    updateDropdowns();
                    rateInfoLabel.setText("Offline: Using hard drive cache.");
                    rateInfoLabel.setStyle("-fx-text-fill: #e74c3c;"); // Red
                });

            } catch (Exception e2) {
                // STEP 3: If Hard Drive fails, use basic Hardcoded rates
                loadHardcodedRates();

                Platform.runLater(() -> {
                    updateDropdowns();
                    rateInfoLabel.setText("Offline: Using basic hardcoded rates.");
                    rateInfoLabel.setStyle("-fx-text-fill: #e74c3c;"); // Red
                });
            }
        }
    }

    private void fetchLiveRates() throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://open.er-api.com/v6/latest/USD"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = mapper.readTree(response.body());
            JsonNode ratesNode = root.path("rates");

            Map<String, Double> newRates = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = ratesNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                newRates.put(field.getKey(), field.getValue().asDouble());
            }

            // Save JSON to hard drive for next time
            mapper.writeValue(new File(CACHE_FILE), newRates);
            exchangeRates = newRates;
        } else {
            // Throw exception so it triggers Step 2
            throw new Exception("API failed to load");
        }
    }

    private void loadCachedRates() throws Exception {
        File file = new File(CACHE_FILE);
        if (!file.exists()) {
            // Throw exception so it triggers Step 3
            throw new Exception("Cache file not found on hard drive");
        }

        JsonNode root = mapper.readTree(file);
        Map<String, Double> cachedRates = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            cachedRates.put(field.getKey(), field.getValue().asDouble());
        }

        exchangeRates = cachedRates;
    }

    private void loadHardcodedRates() {
        exchangeRates = new HashMap<>();
        exchangeRates.put("USD", 1.00);
        exchangeRates.put("EUR", 0.92);
        exchangeRates.put("GBP", 0.79);
        exchangeRates.put("BDT", 110.00);
        exchangeRates.put("INR", 83.20);
        exchangeRates.put("CAD", 1.36);
        exchangeRates.put("AUD", 1.53);
    }

    private void updateDropdowns() {
        ObservableList<String> currencies = FXCollections.observableArrayList(exchangeRates.keySet());
        currencies.sort(String::compareTo);

        String currentFrom = fromCurrencyCombo.getValue();
        String currentTo = toCurrencyCombo.getValue();

        fromCurrencyCombo.setItems(currencies);
        toCurrencyCombo.setItems(currencies);

        fromCurrencyCombo.setValue(currentFrom != null && currencies.contains(currentFrom) ? currentFrom : "USD");
        toCurrencyCombo.setValue(currentTo != null && currencies.contains(currentTo) ? currentTo : "BDT");
    }

    @FXML
    private void handleConvert() {
        errorLabel.setText("");

        try {
            String amountText = amountField.getText();
            if (amountText == null || amountText.trim().isEmpty()) {
                errorLabel.setText("Please enter an amount.");
                return;
            }

            double amount = Double.parseDouble(amountText.trim());
            if (amount < 0) {
                errorLabel.setText("Amount cannot be negative.");
                return;
            }

            String fromCurrency = fromCurrencyCombo.getValue();
            String toCurrency = toCurrencyCombo.getValue();

            if (fromCurrency == null || toCurrency == null) {
                errorLabel.setText("Please select both currencies.");
                return;
            }

            double fromRate = exchangeRates.get(fromCurrency);
            double toRate = exchangeRates.get(toCurrency);
            double amountInUSD = amount / fromRate;
            double finalAmount = amountInUSD * toRate;

            resultLabel.setText(String.format("%.2f %s", finalAmount, toCurrency));

            double singleUnitRate = (1.0 / fromRate) * toRate;
            rateInfoLabel.setText(String.format("1 %s = %.4f %s", fromCurrency, singleUnitRate, toCurrency));

        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter a valid number (e.g. 150.50).");
        }
    }
}