package dev.morling.demos.loom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpHandlers;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.Request;

public class TodoServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class.getName());

    private static final String CONTEXT = "/test";
    private static final Pattern PATH_PATTERN = Pattern.compile("\\" + CONTEXT + "\\/([0-9]+)\\/?");

    private final TodoRepository todoRepository;

    public TodoServer() {
        this.todoRepository = new TodoRepository();
    }

    public void run() throws IOException {
        Predicate<Request> IS_GET = r -> r.getRequestMethod().equals("GET");
        HttpHandler allow = HttpHandlers.of(405, Headers.of("Allow", "GET"), "");

        HttpHandler todoGetHandler = ex -> {
            Optional<Long> todoId = getTodoId(ex);

            if (todoId.isEmpty()) {
                sendErrorResponse(ex, 400, "No valid request");
            }
            else {
                try {
                    String response = todoRepository.getTodo(todoId.get());
                    if (response == null) {
                        sendErrorResponse(ex, 404, "Todo not found");
                    }
                    else {
                        sendResponse(ex, response);
                    }
                }
                catch (Exception e) {
                    LOGGER.error("Request processing failed", e);
                    sendErrorResponse(ex, 500, e.getMessage());
                }
            }
        };

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext(
                CONTEXT,
                HttpHandlers.handleOrElse(IS_GET, todoGetHandler, allow));

//        server.setExecutor(Executors.newCachedThreadPool());
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
    }

    private static Optional<Long> getTodoId(HttpExchange exchange) {
        URI requestURI = exchange.getRequestURI();

        Matcher pathMatcher = PATH_PATTERN.matcher(requestURI.getPath());
        if (!pathMatcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(Long.valueOf(pathMatcher.group(1)));
    }

    private static void sendResponse(HttpExchange exchange, String responseBody) throws IOException {
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);

        exchange.getRequestBody().readAllBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);

        exchange.close();
    }

    private static void sendErrorResponse(HttpExchange exchange, int status, String message) throws IOException {
        message = message.replaceAll("\\r?\\n", "\\\\n")
                .replaceAll("\"", "\\\\\"");

        byte[] bytes = """
                {
                  "errorMessage" : "%s"
                }
                """
                .formatted(message)
                .getBytes(StandardCharsets.UTF_8);

        exchange.getRequestBody().readAllBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);

        exchange.close();
    }
}
