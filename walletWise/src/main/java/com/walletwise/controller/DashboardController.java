package com.walletwise.controller;

import com.walletwise.dao.SettingsDAO;
import com.walletwise.dao.TransactionDAO;
import com.walletwise.model.Transaction;
import com.walletwise.util.CurrencyUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {

    @FXML private ComboBox<String> timeFilterCombo;
    @FXML private Label dateRangeLabel;
    @FXML private HBox customDateContainer;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private Label incomeLabel;
    @FXML private Label expenseLabel;
    @FXML private Label savingsLabel;
    @FXML private Label topCategoryLabel;
    @FXML private Label topCategoryAmountLabel;

    @FXML private PieChart expensePieChart;
    @FXML private BarChart<String, Number> incomeExpenseChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO(); // NEW
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final DateTimeFormatter prettyFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    public void initialize() {
        makeDatePickerTypable(startDatePicker);
        makeDatePickerTypable(endDatePicker);

        timeFilterCombo.setItems(FXCollections.observableArrayList(
                "This Week", "This Month", "Last Month", "This Year", "All Time", "Custom Date Range"
        ));

        // NEW: Load globally saved filter state
        String savedStart = settingsDAO.getSetting("dashboard_start_date");
        String savedEnd = settingsDAO.getSetting("dashboard_end_date");
        if (savedStart != null && !savedStart.isEmpty()) startDatePicker.setValue(LocalDate.parse(savedStart, dateFormatter));
        if (savedEnd != null && !savedEnd.isEmpty()) endDatePicker.setValue(LocalDate.parse(savedEnd, dateFormatter));

        timeFilterCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                settingsDAO.saveSetting("dashboard_time_filter", newV); // Save choice globally
                handleFilterChange(newV);
            }
        });

        // Set Default or Restored View
        String savedFilter = settingsDAO.getSetting("dashboard_time_filter");
        if (savedFilter != null && timeFilterCombo.getItems().contains(savedFilter)) {
            timeFilterCombo.setValue(savedFilter);
        } else {
            timeFilterCombo.setValue("This Month");
        }
    }

    private void handleFilterChange(String filterType) {
        LocalDate today = LocalDate.now();
        LocalDate start = null;
        LocalDate end = null;

        if ("Custom Date Range".equals(filterType)) {
            customDateContainer.setVisible(true);
            customDateContainer.setManaged(true);
            dateRangeLabel.setText("Select dates and click Apply");
            return;
        } else {
            customDateContainer.setVisible(false);
            customDateContainer.setManaged(false);
        }

        switch (filterType) {
            case "This Week":
                start = today.with(DayOfWeek.MONDAY);
                end = today.with(DayOfWeek.SUNDAY);
                break;
            case "This Month":
                start = today.with(TemporalAdjusters.firstDayOfMonth());
                end = today.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case "Last Month":
                LocalDate lastMonth = today.minusMonths(1);
                start = lastMonth.with(TemporalAdjusters.firstDayOfMonth());
                end = lastMonth.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case "This Year":
                start = today.with(TemporalAdjusters.firstDayOfYear());
                end = today.with(TemporalAdjusters.lastDayOfYear());
                break;
            case "All Time":
                start = null;
                end = null;
                break;
        }

        updateDashboardMetrics(start, end);
    }

    @FXML
    private void handleApplyCustomDate() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        // NEW: Save custom dates globally
        settingsDAO.saveSetting("dashboard_start_date", start != null ? dateFormatter.format(start) : "");
        settingsDAO.saveSetting("dashboard_end_date", end != null ? dateFormatter.format(end) : "");

        updateDashboardMetrics(start, end);
    }

    private void updateDashboardMetrics(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            dateRangeLabel.setText("(From the beginning to today)");
        } else if (start != null && end != null) {
            dateRangeLabel.setText("(" + start.format(prettyFormatter) + " - " + end.format(prettyFormatter) + ")");
        } else if (start != null) {
            dateRangeLabel.setText("(From " + start.format(prettyFormatter) + ")");
        } else {
            dateRangeLabel.setText("(Until " + end.format(prettyFormatter) + ")");
        }

        List<Transaction> allTransactions = transactionDAO.getAll();
        double totalIncome = 0;
        double totalExpense = 0;
        Map<String, Double> expenseByCategory = new HashMap<>();

        for (Transaction t : allTransactions) {
            LocalDate d = t.getDate();
            if (d == null) continue;

            if ((start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end))) {
                if ("Income".equalsIgnoreCase(t.getType())) {
                    totalIncome += t.getAmount();
                } else if ("Expense".equalsIgnoreCase(t.getType())) {
                    totalExpense += t.getAmount();
                    expenseByCategory.put(t.getCategoryName(),
                            expenseByCategory.getOrDefault(t.getCategoryName(), 0.0) + t.getAmount());
                }
            }
        }

        String sym = CurrencyUtil.getCurrencySymbol();
        double netSavings = totalIncome - totalExpense;
        incomeLabel.setText(String.format("%s%,.2f", sym, totalIncome));
        expenseLabel.setText(String.format("%s%,.2f", sym, totalExpense));
        savingsLabel.setText(String.format("%s%,.2f", sym, netSavings));

        if (netSavings < 0) {
            savingsLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        } else {
            savingsLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");
        }

        String topCategory = "N/A";
        double highestAmount = 0;
        for (Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
            if (entry.getValue() > highestAmount) {
                highestAmount = entry.getValue();
                topCategory = entry.getKey();
            }
        }

        if (highestAmount > 0) {
            topCategoryLabel.setText(topCategory);
            topCategoryAmountLabel.setText(String.format("%s%,.2f spent", sym, highestAmount));
        } else {
            topCategoryLabel.setText("No Expenses");
            topCategoryAmountLabel.setText(sym + "0.00");
        }

        incomeExpenseChart.getData().clear();

        double maxVal = Math.max(totalIncome, totalExpense);
        if (maxVal == 0) maxVal = 1000;

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(maxVal * 1.25);
        yAxis.setTickUnit(maxVal / 4);

        XYChart.Series<String, Number> cashFlowSeries = new XYChart.Series<>();

        XYChart.Data<String, Number> incomeData = new XYChart.Data<>("Income", totalIncome);
        XYChart.Data<String, Number> expenseData = new XYChart.Data<>("Expense", totalExpense);

        cashFlowSeries.getData().addAll(incomeData, expenseData);
        incomeExpenseChart.getData().add(cashFlowSeries);

        incomeData.getNode().setStyle("-fx-bar-fill: #27ae60;");
        expenseData.getNode().setStyle("-fx-bar-fill: #e74c3c;");

        addValueLabelToBar(incomeData);
        addValueLabelToBar(expenseData);

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey() + " (" + sym + String.format("%,.0f", entry.getValue()) + ")", entry.getValue()));
        }

        expensePieChart.setData(pieChartData);

        for (PieChart.Data data : expensePieChart.getData()) {
            Tooltip tooltip = new Tooltip(data.getName());
            tooltip.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #2c3e50; -fx-text-fill: white; -fx-background-radius: 4;");
            Tooltip.install(data.getNode(), tooltip);
        }
    }

    private void addValueLabelToBar(XYChart.Data<String, Number> data) {
        String sym = CurrencyUtil.getCurrencySymbol();
        StackPane node = (StackPane) data.getNode();
        Text text = new Text(String.format("%s%,.0f", sym, data.getYValue().doubleValue()));
        text.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-fill: #34495e;");

        node.getChildren().add(text);

        StackPane.setAlignment(text, javafx.geometry.Pos.TOP_CENTER);
        text.setTranslateY(-20);
    }

    private void makeDatePickerTypable(DatePicker picker) {
        picker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? dateFormatter.format(date) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.trim().isEmpty()) {
                    try { return LocalDate.parse(string.trim(), dateFormatter); }
                    catch (Exception e) { return picker.getValue(); }
                }
                return null;
            }
        });

        picker.getEditor().focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                try {
                    String text = picker.getEditor().getText();
                    if (text == null || text.trim().isEmpty()) picker.setValue(null);
                    else picker.setValue(LocalDate.parse(text.trim(), dateFormatter));
                } catch (Exception e) {
                    picker.getEditor().setText(picker.getConverter().toString(picker.getValue()));
                }
            }
        });
    }
}