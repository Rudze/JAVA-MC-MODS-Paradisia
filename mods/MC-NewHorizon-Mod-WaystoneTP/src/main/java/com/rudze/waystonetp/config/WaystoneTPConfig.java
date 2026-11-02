package com.rudze.waystonetp.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WaystoneTPConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Waystone Teleport module: right-click a Waystone A to warp to a random Waystone B, and vice versa.")
                .push("waystoneTeleport");

        ENABLED = builder
                .comment("Master switch for the Waystone Teleport module.")
                .define("enabled", true);

        builder.pop();
        SPEC = builder.build();
    }

    private WaystoneTPConfig() {
    }
}
