package com.globalexchange.server;

import com.globalexchange.service.ExchangeRateService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ExchangeServer {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isEmpty()) {
            try {
                port = Integer.parseInt(envPort);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable. Using default: " + DEFAULT_PORT);
            }
        }

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Context for APIs
            server.createContext("/api/convert", new ConvertHandler());
            server.createContext("/api/rates", new RatesHandler());
            server.createContext("/api/currencies", new CurrenciesHandler());

            // Context for Static Files (frontend)
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(null); // default executor
            System.out.println("\n==================================================");
            System.out.println("   GLOBAL EXCHANGE SERVER STARTED SUCCESSFULLY    ");
            System.out.println("==================================================");
            System.out.println(" Port: " + port);
            System.out.println(" Dashboard URL: http://localhost:" + port);
            System.out.println(" API Convert:   http://localhost:" + port + "/api/convert?from=USD&to=INR&amount=100");
            System.out.println(" API Rates:     http://localhost:" + port + "/api/rates?base=USD");
            System.out.println(" API List:      http://localhost:" + port + "/api/currencies");
            System.out.println("==================================================\n");
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length > 1) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                params.put(key, value);
            } else if (keyValue.length == 1) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                params.put(key, "");
            }
        }
        return params;
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    static class ConvertHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Only GET method is allowed\"}");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String from = params.get("from");
            String to = params.get("to");
            String amountStr = params.get("amount");

            if (from == null || to == null || amountStr == null || from.isEmpty() || to.isEmpty() || amountStr.isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Missing required parameters: from, to, amount\"}");
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                double rate = ExchangeRateService.getExchangeRate(from.toUpperCase(), to.toUpperCase());
                double result = amount * rate;

                // Return formatted response string (avoid scientific notation issues for floats)
                String response = String.format(
                        "{\"from\":\"%s\",\"to\":\"%s\",\"amount\":%.2f,\"rate\":%.6f,\"result\":%.2f}",
                        from.toUpperCase(), to.toUpperCase(), amount, rate, result
                );
                sendJsonResponse(exchange, 200, response);
            } catch (NumberFormatException e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Invalid amount format. Must be a number.\"}");
            } catch (IllegalArgumentException e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Error connecting to rates service: " + e.getMessage() + "\"}");
            }
        }
    }

    static class RatesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Only GET method is allowed\"}");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String base = params.getOrDefault("base", "USD").toUpperCase();

            try {
                String jsonData = ExchangeRateService.fetchJsonData(base);
                Map<String, Double> rates = ExchangeRateService.parseRates(jsonData);
                String date = ExchangeRateService.parseLastUpdatedDate(jsonData);

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("{\"base\":\"%s\",\"date\":\"%s\",\"rates\":{", base, date));
                boolean first = true;
                for (String key : ExchangeRateService.getSupportedCurrencies().keySet()) {
                    if (rates.containsKey(key)) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append(String.format("\"%s\":%.6f", key, rates.get(key)));
                    }
                }
                sb.append("}}");
                sendJsonResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Failed to fetch rates: " + e.getMessage() + "\"}");
            }
        }
    }

    static class CurrenciesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Only GET method is allowed\"}");
                return;
            }

            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, ExchangeRateService.CurrencyInfo> entry : ExchangeRateService.getSupportedCurrencies().entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(String.format("\"%s\":{\"name\":\"%s\",\"symbol\":\"%s\"}",
                        entry.getKey(),
                        entry.getValue().name,
                        entry.getValue().symbol));
            }
            sb.append("}");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            // Prevent path traversal security vulnerability
            path = path.replace("..", "");

            File file = new File("web" + path);
            if (!file.exists() || file.isDirectory()) {
                String response = "404 (Not Found)\nFile: " + path;
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String contentType = "text/plain; charset=utf-8";
            if (path.endsWith(".html")) contentType = "text/html; charset=utf-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=utf-8";
            else if (path.endsWith(".js")) contentType = "text/javascript; charset=utf-8";
            else if (path.endsWith(".png")) contentType = "image/png";
            else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml; charset=utf-8";
            else if (path.endsWith(".ico")) contentType = "image/x-icon";

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, count);
                }
            }
        }
    }
}
