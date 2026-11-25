package controller;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import service.InventoryService;

public class InventoryController implements HttpHandler{
	private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("POST".equalsIgnoreCase(method) && "/api/usage".equals(path)) {
                sendJson(exchange, 201, addUsage(exchange));
                return;
            }

            if ("PUT".equalsIgnoreCase(method) && "/api/usage".equals(path)) {
                sendJson(exchange, 200, updateUsage(exchange));
                return;
            }

            if (!"GET".equalsIgnoreCase(method)) {
                sendJson(exchange, 405, Map.of("message", "Method not allowed."));
                return;
            }

            switch (path) {
                case "/api/usage" ->
                        sendJson(exchange, 200, service.getUsage());
                case "/api/ingredients" ->
                        sendJson(exchange, 200, service.getIngredients());
                case "/api/analysis" ->
                        sendJson(exchange, 200, analyze(exchange));
                default ->
                        sendJson(exchange, 404, Map.of("message", "Not found."));
            }
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("message", e.getMessage()));
        } catch (DateTimeParseException e) {
            sendJson(exchange, 400, Map.of("message", "Dates must use YYYY-MM-DD format."));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("message", "Server error: " + e.getMessage()));
        }
    }

    private Map<String, Object> analyze(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(
                exchange.getRequestURI().getRawQuery());

        String ingredient = params.get("ingredient");
        String fromValue = params.get("from");
        String toValue = params.get("to");

        if (ingredient == null || fromValue == null || toValue == null) {
            throw new IllegalArgumentException(
                    "Query requires ingredient, from, and to parameters.");
        }

        LocalDate from = LocalDate.parse(fromValue);
        LocalDate to = LocalDate.parse(toValue);

        return service.analyze(ingredient, from, to);
    }

    private Object addUsage(HttpExchange exchange) throws IOException {
        Map<String, String> params = readForm(exchange);
        return service.addUsage(createRecord(
                params.get("date"), params.get("ingredient"), params.get("quantity")));
    }

    private Object updateUsage(HttpExchange exchange) throws IOException {
        Map<String, String> params = readForm(exchange);
        LocalDate originalDate = parseDate(params.get("originalDate"), "Original date is required.");
        String originalIngredient = required(params.get("originalIngredient"), "Original ingredient is required.");
        double originalQuantity = parseQuantity(params.get("originalQuantity"));
        model.UsageRecord original = new model.UsageRecord(
                originalDate, originalIngredient, originalQuantity);
        model.UsageRecord updated = createRecord(
                params.get("date"), params.get("ingredient"), params.get("quantity"));
        return service.updateUsage(original, updated);
    }

    private Map<String, String> readForm(HttpExchange exchange) throws IOException {
        try (var input = exchange.getRequestBody()) {
            String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return parseQuery(body);
        }
    }

    private model.UsageRecord createRecord(String dateValue, String ingredientValue, String quantityValue) {
        if (dateValue == null || ingredientValue == null || quantityValue == null) {
            throw new IllegalArgumentException(
                    "Date, ingredient, and quantity are required.");
        }

        LocalDate date = parseDate(dateValue, "Date is required.");
        String ingredient = ingredientValue.trim();
        if (ingredient.contains(",") || ingredient.contains("\n") || ingredient.contains("\r")) {
            throw new IllegalArgumentException(
                    "Ingredient cannot contain commas or line breaks.");
        }

        double quantity = parseQuantity(quantityValue);
        return new model.UsageRecord(date, ingredient, quantity);
    }

    private LocalDate parseDate(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return LocalDate.parse(value);
    }

    private double parseQuantity(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Quantity is required.");
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantity must be a number.");
        }
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private Map<String, String> parseQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query parameters are required.");
        }

        return List.of(query.split("&")).stream()
                .map(part -> part.split("=", 2))
                .filter(pair -> pair.length == 2)
                .collect(Collectors.toMap(
                        pair -> decode(pair[0]),
                        pair -> decode(pair[1]),
                        (first, second) -> second
                ));
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJson(HttpExchange exchange, int status, Object body)
            throws IOException {
        String json = Json.stringify(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
