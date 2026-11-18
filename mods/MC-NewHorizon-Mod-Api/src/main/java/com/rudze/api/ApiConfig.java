package com.rudze.api;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ApiConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.ConfigValue<String> BIND_ADDRESS;
    public static final ModConfigSpec.IntValue PORT;
    public static final ModConfigSpec.ConfigValue<String> TOKEN;
    public static final ModConfigSpec.ConfigValue<String> ALLOWED_ORIGIN;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Read-only HTTP API for the Paradisia website.").push("api");

        ENABLED = builder
                .comment("Start the HTTP server when the Minecraft server starts.")
                .define("enabled", true);
        BIND_ADDRESS = builder
                .comment("Address the HTTP server listens on. 127.0.0.1 = this machine only (behind an HTTPS reverse proxy), 0.0.0.0 = every interface.")
                .define("bindAddress", "127.0.0.1");
        PORT = builder
                .comment("TCP port of the HTTP server.")
                .defineInRange("port", 8091, 1, 65535);
        TOKEN = builder
                .comment("Optional secret key. Empty = public API (the data is not sensitive).",
                        "Set, every /api request but /api/health must send it as \"Authorization: Bearer <token>\" or \"X-Api-Key: <token>\".")
                .define("token", "");
        ALLOWED_ORIGIN = builder
                .comment("Value of the Access-Control-Allow-Origin header, so a website (GitHub Pages) can call the API",
                        "from the browser. \"*\" = any site, empty = no CORS header.")
                .define("allowedOrigin", "*");

        builder.pop();
        SPEC = builder.build();
    }

    private ApiConfig() {
    }
}
