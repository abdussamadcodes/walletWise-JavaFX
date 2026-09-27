package com.walletwise.util;

import com.walletwise.dao.SettingsDAO;

public class CurrencyUtil {
    private static final SettingsDAO settingsDAO = new SettingsDAO();

    public static String getCurrencyCode() {
        String code = settingsDAO.getSetting("app_currency_code");
        return code != null ? code : "BDT";
    }

    public static String getCurrencySymbol() {
        String symbol = settingsDAO.getSetting("app_currency_symbol");
        return symbol != null ? symbol : "৳";
    }

    public static void saveCurrency(String code) {
        String symbol = "৳";
        switch (code) {
            case "USD": case "CAD": case "AUD": symbol = "$"; break;
            case "EUR": symbol = "€"; break;
            case "GBP": symbol = "£"; break;
            case "INR": symbol = "₹"; break;
            case "BDT": default: symbol = "৳"; break;
        }
        settingsDAO.saveSetting("app_currency_code", code);
        settingsDAO.saveSetting("app_currency_symbol", symbol);
    }
}