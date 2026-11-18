package com.rudze.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Read-only HTTP server. Every response is JSON.
 *
 * <ul>
 *   <li>{@code GET /api/health} - no key needed: is the server up, how many players are online.</li>
 *   <li>{@code GET /api/players} - every player who ever joined.</li>
 *   <li>{@code GET /api/players/<uuid or name>} - one player with their balance and Pokemon counts.</li>
 *   <li>{@code GET /api/players/<uuid or name>/pokemon} - their party and PC boxes.</li>
 * </ul>
 *
 * The API is public unless a token is set in config/nh_api.toml; then the other routes need it, as
 * {@code Authorization: Bearer <key>} or {@code X-Api-Key: <key>}. Requests are handled on a small thread pool; anything touching the game
 * is run on the server thread.
 */
final class ApiServer {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final MinecraftServer minecraft;
    private final PlayerData players;
    private HttpServer http;
    private ExecutorService executor;

    ApiServer(MinecraftServer minecraft) {
        this.minecraft = minecraft;
        this.players = new PlayerData(minecraft);
    }

    void start(String address, int port) throws IOException {
        http = HttpServer.create(new InetSocketAddress(address, port), 0);
        executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "nh_api-http");
            thread.setDaemon(true);
            return thread;
        });
        http.setExecutor(executor);
        http.createContext("/api/", this::handle);
        http.start();
        ApiMod.LOGGER.info("Web API listening on {}:{}", address, port);
    }

    void stop() {
        http.stop(0);
        executor.shutdownNow();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String origin = ApiConfig.ALLOWED_ORIGIN.get();
            if (!origin.isBlank()) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, X-Api-Key");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            }
            String method = exchange.getRequestMethod();
            if (method.equals("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!method.equals("GET")) {
                send(exchange, 405, error("Only GET is supported"));
                return;
            }
            String[] path = exchange.getRequestURI().getPath().replaceAll("/+$", "").split("/");
            // path = ["", "api", ...]
            if (path.length == 3 && path[2].equals("health")) {
                send(exchange, 200, onServerThread(players::health));
                return;
            }
            if (!authorized(exchange)) {
                send(exchange, 401, error("Missing or wrong API key"));
                return;
            }
            if (path.length == 3 && path[2].equals("players")) {
                send(exchange, 200, onServerThread(players::listPlayers));
            } else if (path.length >= 4 && path[2].equals("players")) {
                String who = URLDecoder.decode(path[3], StandardCharsets.UTF_8);
                Optional<UUID> uuid = onServerThread(() -> players.resolve(who));
                if (uuid.isEmpty()) {
                    send(exchange, 404, error("No player " + who + " ever joined"));
                } else if (path.length == 4) {
                    send(exchange, 200, onServerThread(() -> players.player(uuid.get())));
                } else if (path.length == 5 && path[4].equals("pokemon")) {
                    send(exchange, 200, onServerThread(() -> players.pokemon(uuid.get())));
                } else {
                    send(exchange, 404, error("Unknown route"));
                }
            } else {
                send(exchange, 404, error("Unknown route"));
            }
        } catch (Exception e) {
            ApiMod.LOGGER.warn("Web API request {} failed", exchange.getRequestURI(), e);
            try {
                send(exchange, 500, error("Internal error"));
            } catch (IOException ignored) {
                // The response was already started.
            }
        }
    }

    private <T> T onServerThread(Supplier<T> task) throws Exception {
        return minecraft.submit(task).get(10, TimeUnit.SECONDS);
    }

    private static boolean authorized(HttpExchange exchange) {
        String expected = ApiConfig.TOKEN.get();
        if (expected.isBlank()) {
            return true;   // public API
        }
        String given = exchange.getRequestHeaders().getFirst("X-Api-Key");
        String bearer = exchange.getRequestHeaders().getFirst("Authorization");
        if (given == null && bearer != null && bearer.startsWith("Bearer ")) {
            given = bearer.substring("Bearer ".length()).trim();
        }
        return given != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }

    private static JsonObject error(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("error", message);
        return json;
    }

    private static void send(HttpExchange exchange, int status, JsonElement body) throws IOException {
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
