package com.globalexchange.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExchangeRateService {

    public static class CurrencyInfo {
        public final String code;
        public final String name;
        public final String symbol;

        public CurrencyInfo(String code, String name, String symbol) {
            this.code = code;
            this.name = name;
            this.symbol = symbol;
        }
    }

    private static final Map<String, CurrencyInfo> SUPPORTED_CURRENCIES = new HashMap<>();

    static {
        SUPPORTED_CURRENCIES.put("USD", new CurrencyInfo("USD", "US Dollar", "$"));
        SUPPORTED_CURRENCIES.put("EUR", new CurrencyInfo("EUR", "Euro", "€"));
        SUPPORTED_CURRENCIES.put("GBP", new CurrencyInfo("GBP", "British Pound", "£"));
        SUPPORTED_CURRENCIES.put("INR", new CurrencyInfo("INR", "Indian Rupee", "₹"));
        SUPPORTED_CURRENCIES.put("JPY", new CurrencyInfo("JPY", "Japanese Yen", "¥"));
        SUPPORTED_CURRENCIES.put("AUD", new CurrencyInfo("AUD", "Australian Dollar", "A$"));
        SUPPORTED_CURRENCIES.put("CAD", new CurrencyInfo("CAD", "Canadian Dollar", "C$"));
        SUPPORTED_CURRENCIES.put("SGD", new CurrencyInfo("SGD", "Singapore Dollar", "S$"));
        SUPPORTED_CURRENCIES.put("CHF", new CurrencyInfo("CHF", "Swiss Franc", "CHF"));
        SUPPORTED_CURRENCIES.put("CNY", new CurrencyInfo("CNY", "Chinese Yuan", "¥"));
        SUPPORTED_CURRENCIES.put("HKD", new CurrencyInfo("HKD", "Hong Kong Dollar", "HK$"));
        SUPPORTED_CURRENCIES.put("NZD", new CurrencyInfo("NZD", "New Zealand Dollar", "NZ$"));
        SUPPORTED_CURRENCIES.put("SEK", new CurrencyInfo("SEK", "Swedish Krona", "kr"));
        SUPPORTED_CURRENCIES.put("NOK", new CurrencyInfo("NOK", "Norwegian Krone", "kr"));
        SUPPORTED_CURRENCIES.put("DKK", new CurrencyInfo("DKK", "Danish Krone", "kr"));
        SUPPORTED_CURRENCIES.put("RUB", new CurrencyInfo("RUB", "Russian Ruble", "₽"));
        SUPPORTED_CURRENCIES.put("ZAR", new CurrencyInfo("ZAR", "South African Rand", "R"));
        SUPPORTED_CURRENCIES.put("BRL", new CurrencyInfo("BRL", "Brazilian Real", "R$"));
        SUPPORTED_CURRENCIES.put("MXN", new CurrencyInfo("MXN", "Mexican Peso", "$"));
        SUPPORTED_CURRENCIES.put("AED", new CurrencyInfo("AED", "UAE Dirham", "AED"));
        SUPPORTED_CURRENCIES.put("SAR", new CurrencyInfo("SAR", "Saudi Riyal", "SR"));
        SUPPORTED_CURRENCIES.put("TRY", new CurrencyInfo("TRY", "Turkish Lira", "₺"));
        SUPPORTED_CURRENCIES.put("KRW", new CurrencyInfo("KRW", "South Korean Won", "₩"));
        SUPPORTED_CURRENCIES.put("IDR", new CurrencyInfo("IDR", "Indonesian Rupiah", "Rp"));
        SUPPORTED_CURRENCIES.put("MYR", new CurrencyInfo("MYR", "Malaysian Ringgit", "RM"));
        SUPPORTED_CURRENCIES.put("PHP", new CurrencyInfo("PHP", "Philippine Peso", "₱"));
        SUPPORTED_CURRENCIES.put("THB", new CurrencyInfo("THB", "Thai Baht", "฿"));
        SUPPORTED_CURRENCIES.put("VND", new CurrencyInfo("VND", "Vietnamese Dong", "₫"));
        SUPPORTED_CURRENCIES.put("PKR", new CurrencyInfo("PKR", "Pakistani Rupee", "₨"));
        SUPPORTED_CURRENCIES.put("EGP", new CurrencyInfo("EGP", "Egyptian Pound", "E£"));
        SUPPORTED_CURRENCIES.put("PLN", new CurrencyInfo("PLN", "Polish Zloty", "zł"));
    }

    public static Map<String, CurrencyInfo> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    public static String fetchJsonData(String baseCurrency) throws Exception {
        String apiURL = "https://api.exchangerate-api.com/v4/latest/" + baseCurrency;
        URL url = new URL(apiURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Failed to fetch exchange rates. Code: " + connection.getResponseCode());
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    public static Map<String, Double> parseRates(String json) {
        Map<String, Double> rates = new HashMap<>();
        int ratesIndex = json.indexOf("\"rates\"");
        if (ratesIndex == -1) {
            return rates;
        }
        
        String ratesPart = json.substring(ratesIndex);
        Pattern pattern = Pattern.compile("\"([A-Z]{3})\":\\s*([0-9.]+)");
        Matcher matcher = pattern.matcher(ratesPart);
        
        while (matcher.find()) {
            String currency = matcher.group(1);
            double rate = Double.parseDouble(matcher.group(2));
            rates.put(currency, rate);
        }
        return rates;
    }

    public static String parseLastUpdatedDate(String json) {
        Pattern datePattern = Pattern.compile("\"date\":\\s*\"([^\"]+)\"");
        Matcher dateMatcher = datePattern.matcher(json);
        return dateMatcher.find() ? dateMatcher.group(1) : "N/A";
    }

    public static double getExchangeRate(String base, String target) throws Exception {
        String json = fetchJsonData(base);
        Map<String, Double> rates = parseRates(json);
        if (!rates.containsKey(target)) {
            throw new IllegalArgumentException("Invalid target currency: " + target);
        }
        return rates.get(target);
    }
}
