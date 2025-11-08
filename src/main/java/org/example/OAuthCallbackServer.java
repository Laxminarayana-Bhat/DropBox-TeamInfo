package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OAuthCallbackServer {
    private final HttpServer server;
    private final CompletableFuture<String> codeFuture = new CompletableFuture<>();

    public OAuthCallbackServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/callback", new CallbackHandler());
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public String waitForCode(long timeout, TimeUnit unit) throws Exception {
        try {
            return codeFuture.get(timeout, unit);
        } catch (TimeoutException te) {
            throw new RuntimeException("Timed out waiting for authorization code.");
        }
    }

    private class CallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String responseHtml;
            if (query != null && query.contains("code=")) {
                String code = getParam(query, "code");
                codeFuture.complete(code);
                responseHtml = "<html><body><h3>Authorization successful. You can close this window.</h3></body></html>";
            } else if (query != null && query.contains("error=")) {
                String err = getParam(query, "error");
                codeFuture.completeExceptionally(new RuntimeException("Authorization error: " + err));
                responseHtml = "<html><body><h3>Authorization failed or denied.</h3></body></html>";
            } else {
                codeFuture.completeExceptionally(new RuntimeException("Missing code in callback."));
                responseHtml = "<html><body><h3>Invalid callback response.</h3></body></html>";
            }
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            byte[] bytes = responseHtml.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String getParam(String q, String name) {
            for (String part : q.split("&")) {
                int idx = part.indexOf('=');
                if (idx > 0) {
                    String k = part.substring(0, idx);
                    String v = part.substring(idx + 1);
                    if (k.equals(name)) {
                        return URLDecoder.decode(v, StandardCharsets.UTF_8);
                    }
                }
            }
            return null;
        }
    }
}

