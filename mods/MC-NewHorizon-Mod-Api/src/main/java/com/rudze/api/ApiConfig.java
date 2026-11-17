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
                .comment("Address the HTTP server listens on. 0.0.0.0 = every interface, 127.0.0.1 = this machine only.")
                .define("bindAddress", "0.0.0.0");
        PORT = builder
                .comment("TCP port of the HTTP server.")
                .defineInRange("port", 8091, 1, 65535);
        TOKEN = builder
                .comment("Secret key every /api request must send, as \"Authorization: Bearer <token>\" or \"X-Api-Key: <token>\".",
                        "Left empty, a random one is generated on first start and written here.")
                .define("token", "");
        ALLOWED_ORIGIN = builder
                .comment("Value of the Access-Control-Allow-Origin header, for a website calling the API from the browser",
                        "(for example https://paradisia.fr). Empty = no CORS header.")
                .define("allowedOrigin", "");

        builder.pop();
        SPEC = builder.build();
    }

    private ApiConfig() {
    }
}
