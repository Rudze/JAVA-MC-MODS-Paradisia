package tallestred.numismaticoverhaul.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tallestred.numismaticoverhaul.NumismaticOverhaul;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * A tiny local-only HTTP dashboard for {@link tallestred.numismaticoverhaul.entity.WheatMerchantEntity}
 * markets, serving a static HTML/canvas page (no external JS/CSS - the server itself has no reason to
 * assume outbound internet access) that polls a JSON endpoint every couple seconds and redraws each
 * merchant's price-over-time curve. Built on the JDK's own {@code com.sun.net.httpserver} rather than
 * pulling in a web framework dependency for what's a handful of read-only routes.
 *
 * <p>Reads exclusively from {@link EconomyRegistry}'s already-thread-safe snapshot map - the HTTP
 * handler threads never touch the actual world/entities, since that's only safe from the server
 * thread.
 */
public final class EconomyWebServer {
    // Not currently exposed as a config option - see the class javadoc for why an eventual config
    // entry would be the natural next step if this needs to run on a dedicated server reachable by
    // more than the host machine.
    private static final int PORT = 8123;
    private static final Gson GSON = new GsonBuilder().create();

    private static HttpServer server;
    private static byte[] dashboardHtml;

    private EconomyWebServer() {
    }

    public static void start() {
        if (server != null) return;

        try {
            dashboardHtml = loadDashboardHtml();
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
            server.createContext("/", EconomyWebServer::serveDashboard);
            server.createContext("/api/prices", EconomyWebServer::serveApi);
            server.createContext("/portraits/", EconomyWebServer::servePortrait);
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "numismaticoverhaul-economy-dashboard");
                thread.setDaemon(true);
                return thread;
            }));
            server.start();
            NumismaticOverhaul.LOGGER.info("Wheat market dashboard running at http://localhost:{}/", PORT);
        } catch (IOException e) {
            NumismaticOverhaul.LOGGER.error("Failed to start the economy dashboard web server on port {}", PORT, e);
            server = null;
        }
    }

    public static void stop() {
        if (server == null) return;
        server.stop(0);
        server = null;
    }

    private static byte[] loadDashboardHtml() throws IOException {
        try (InputStream stream = EconomyWebServer.class.getResourceAsStream("/dashboard/index.html")) {
            if (stream == null) throw new IOException("dashboard/index.html missing from mod resources");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            stream.transferTo(buffer);
            return buffer.toByteArray();
        }
    }

    private static void serveDashboard(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, dashboardHtml.length);
        try (var body = exchange.getResponseBody()) {
            body.write(dashboardHtml);
        }
    }

    // Serves portrait images from mod resources (dashboard/portraits/<file>) rather than bundling
    // them into index.html as data URIs - keeps the HTML readable and lets merchants without a
    // shop yet still show a real portrait via the static catalog in index.html, with no Java change
    // needed to add one.
    private static void servePortrait(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String fileName = path.substring("/portraits/".length());
        if (fileName.isEmpty() || fileName.contains("/") || fileName.contains("..")) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        try (InputStream stream = EconomyWebServer.class.getResourceAsStream("/dashboard/portraits/" + fileName)) {
            if (stream == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] bytes = stream.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var body = exchange.getResponseBody()) {
                body.write(bytes);
            }
        }
    }

    private static void serveApi(HttpExchange exchange) throws IOException {
        JsonArray merchants = new JsonArray();
        for (EconomyRegistry.Snapshot snapshot : EconomyRegistry.snapshotAll()) {
            JsonObject merchantJson = new JsonObject();
            merchantJson.addProperty("id", snapshot.id());
            merchantJson.addProperty("name", snapshot.name());
            merchantJson.addProperty("portrait", snapshot.portrait());

            JsonArray commoditiesJson = new JsonArray();
            for (EconomyRegistry.CommoditySnapshot commodity : snapshot.commodities()) {
                JsonObject commodityJson = new JsonObject();
                commodityJson.addProperty("id", commodity.id());
                commodityJson.addProperty("name", commodity.name());
                commodityJson.addProperty("item", commodity.item());
                commodityJson.addProperty("stock", commodity.stock());
                commodityJson.addProperty("price", commodity.price());

                JsonArray historyJson = new JsonArray();
                for (PricePoint point : commodity.history()) {
                    JsonObject pointJson = new JsonObject();
                    pointJson.addProperty("time", point.gameTime());
                    pointJson.addProperty("realTime", point.realTime());
                    pointJson.addProperty("price", point.price());
                    pointJson.addProperty("stock", point.stock());
                    historyJson.add(pointJson);
                }
                commodityJson.add("history", historyJson);
                commoditiesJson.add(commodityJson);
            }
            merchantJson.add("commodities", commoditiesJson);
            merchants.add(merchantJson);
        }

        byte[] body = GSON.toJson(merchants).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (var responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
    }
}
