package com.rudze.deathpenalty.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DeathPenaltyConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue RESPAWN_DELAY_SECONDS;
    public static final ModConfigSpec.BooleanValue GREY_OUT_RESPAWN_BUTTON;
    public static final ModConfigSpec.DoubleValue RESPAWN_HEARTS;
    public static final ModConfigSpec.IntValue RESPAWN_HUNGER_ICONS;
    public static final ModConfigSpec.DoubleValue RESPAWN_SATURATION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Death Penalty module: punishes dying with a respawn delay and reduced stats on respawn.")
                .push("deathPenalty");

        ENABLED = builder
                .comment("Master switch for the Death Penalty module.")
                .define("enabled", true);

        builder.push("respawnDelay");
        RESPAWN_DELAY_SECONDS = builder
                .comment("How long, in seconds, the respawn button stays disabled after dying. 600 = 10 minutes.")
                .defineInRange("respawnDelaySeconds", 600, 0, 86400);
        GREY_OUT_RESPAWN_BUTTON = builder
                .comment("Client-side only: grey out the death screen's Respawn button and show a countdown on it",
                        "for the duration of respawnDelaySeconds. Purely visual/UX; does not affect the stat penalty below.")
                .define("greyOutRespawnButton", true);
        builder.pop();

        builder.push("respawnStats");
        RESPAWN_HEARTS = builder
                .comment("Health the player respawns with, in hearts (2 health points per heart). Default 3.0 = 3 full hearts.")
                .defineInRange("respawnHearts", 3.0, 0.5, 50.0);
        RESPAWN_HUNGER_ICONS = builder
                .comment("Food level the player respawns with, in hunger icons (2 food points per icon, 10 icons max).",
                        "Default 3 = 3 full hunger icons.")
                .defineInRange("respawnHungerIcons", 3, 0, 10);
        RESPAWN_SATURATION = builder
                .comment("Saturation the player respawns with. 0 means the hunger icons above start depleting immediately.")
                .defineInRange("respawnSaturation", 0.0, 0.0, 20.0);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    private DeathPenaltyConfig() {
    }
}
