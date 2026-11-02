package com.rudze.backpack.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BackpackConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue ROWS;

    public static final ModConfigSpec.DoubleValue RENDER_SCALE;
    public static final ModConfigSpec.DoubleValue RENDER_OFFSET_X;
    public static final ModConfigSpec.DoubleValue RENDER_OFFSET_Y;
    public static final ModConfigSpec.DoubleValue RENDER_OFFSET_Z;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Backpack module: a wearable storage item, worn via the Accessories mod's back slot.")
                .push("backpack");

        ENABLED = builder
                .comment("Master switch for the Backpack module. Requires the Accessories mod to be installed.")
                .define("enabled", true);
        ROWS = builder
                .comment("Number of 9-wide inventory rows the backpack holds (1-6, like a chest).")
                .defineInRange("rows", 3, 1, 6);

        builder.push("render");
        RENDER_SCALE = builder
                .comment("Size multiplier applied when the backpack is rendered on the player's back.",
                        "Default matches a working reference implementation's own tuned value.",
                        "This file is hot-reloaded: you can tweak this while the game is running and see the change",
                        "on your next respawn/re-equip without restarting.")
                .defineInRange("renderScale", 0.8, 0.1, 20.0);
        RENDER_OFFSET_X = builder
                .comment("Extra X offset (in blocks) applied after anchoring to the player's back and scaling.")
                .defineInRange("renderOffsetX", 0.0, -8.0, 8.0);
        RENDER_OFFSET_Y = builder
                .comment("Extra Y offset (in blocks) applied after anchoring to the player's back and scaling.")
                .defineInRange("renderOffsetY", 7.0 / 16.0, -8.0, 8.0);
        RENDER_OFFSET_Z = builder
                .comment("Extra Z offset (in blocks) applied after anchoring to the player's back and scaling.")
                .defineInRange("renderOffsetZ", 6.0 / 16.0, -8.0, 8.0);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    private BackpackConfig() {
    }
}
