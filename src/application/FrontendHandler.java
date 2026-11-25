package application;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Serves the static browser pages and assets from the frontend directory. */
public class FrontendHandler implements HttpHandler {
    private static final Map<String, String> PAGES = Map.of(
            "/", "index.html",
            "/index.html", "index.html",
            "/records.html", "records.html",
            "/analysis.html", "analysis.html",
            "/style.css", "style.css",
            "/app.js", "app.js"
    );

    private final Path frontendDirectory;

    public FrontendHandler(Path frontendDirectory) {
        this.frontendDirectory = frontendDirectory.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=UTF-8", "Only GET is supported.");
            return;
        }

        String fileName = PAGES.get(exchange.getRequestURI().getPath());
        if (fileName == null) {
            send(exchange, 404, "text/plain; charset=UTF-8", "Page not found.");
            return;
        }

        Path file = frontendDirectory.resolve(fileName).normalize();
        if (!file.startsWith(frontendDirectory) || !Files.isRegularFile(file)) {
            send(exchange, 404, "text/plain; charset=UTF-8", "Frontend file not found.");
            return;
        }

        String contentType = fileName.endsWith(".html")
                ? "text/html; charset=UTF-8"
                : fileName.endsWith(".css")
                    ? "text/css; charset=UTF-8"
                    : "text/javascript; charset=UTF-8";

        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void send(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
