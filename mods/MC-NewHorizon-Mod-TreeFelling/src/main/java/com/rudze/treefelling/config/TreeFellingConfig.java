package com.rudze.treefelling.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.regex.Pattern;

public final class TreeFellingConfig {

    public enum FallMode {
        INSTANT,
        PROGRESSIVE
    }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue REQUIRE_AXE;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_LOG_BLOCKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_LOG_BLOCKS;
    public static final ModConfigSpec.BooleanValue USE_26_DIRECTIONS;
    public static final ModConfigSpec.IntValue MAX_TREE_SIZE;
    public static final ModConfigSpec.BooleanValue REQUIRE_LEAVES_NEARBY;
    public static final ModConfigSpec.IntValue LEAVES_SEARCH_RADIUS;

    public static final ModConfigSpec.EnumValue<FallMode> FALL_MODE;
    public static final ModConfigSpec.IntValue PROGRESSIVE_DELAY_TICKS;

    public static final ModConfigSpec.DoubleValue BASE_TIME_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue PER_LOG_TIME_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MAX_TIME_MULTIPLIER;

    private static final Pattern RESOURCE_LOCATION = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");

    private static boolean isValidBlockId(Object entry) {
        return entry instanceof String string && RESOURCE_LOCATION.matcher(string).matches();
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Tree Felling module: cut one log, bring down the whole tree.").push("treeFelling");

        ENABLED = builder
                .comment("Master switch for the Tree Felling module.")
                .define("enabled", true);
        REQUIRE_AXE = builder
                .comment("Require the player to be holding an axe for the felling effect to trigger.",
                        "If false, breaking a log with any tool fells the tree.")
                .define("requireAxe", true);

        builder.push("detection");
        EXTRA_LOG_BLOCKS = builder
                .comment("Extra block IDs (registry names) considered part of a tree trunk, in addition to the",
                        "minecraft:logs tag. Useful for modded log-like blocks not covered by the tag.",
                        "Example: \"othermod:sacred_log\"")
                .defineListAllowEmpty("extraLogBlocks", List.of(), () -> "minecraft:oak_log", TreeFellingConfig::isValidBlockId);
        EXCLUDED_LOG_BLOCKS = builder
                .comment("Block IDs excluded from tree detection even though they are in minecraft:logs.",
                        "Useful to keep stripped logs used in builds from being felled.",
                        "Example: \"minecraft:stripped_oak_log\"")
                .defineListAllowEmpty("excludedLogBlocks", List.of(), () -> "minecraft:stripped_oak_log", TreeFellingConfig::isValidBlockId);
        USE_26_DIRECTIONS = builder
                .comment("Connect logs using all 26 neighbouring positions instead of just the 6 orthogonal ones.",
                        "Needed to correctly detect trees with diagonal trunks/branches (acacia, jungle, dark oak canopies).")
                .define("use26Directions", true);
        MAX_TREE_SIZE = builder
                .comment("Maximum number of logs scanned above the broken log. Search stops once this cap is reached,",
                        "protecting against lag/abuse on giant artificial structures.")
                .defineInRange("maxTreeSize", 512, 1, 4096);
        REQUIRE_LEAVES_NEARBY = builder
                .comment("Require leaves near the top of the trunk before felling it. Prevents wooden houses/builds",
                        "from being treated as trees. When false or when no leaves are found, breaking a log falls",
                        "back to vanilla single-block behaviour (the felling effect simply does not trigger).")
                .define("requireLeavesNearby", true);
        LEAVES_SEARCH_RADIUS = builder
                .comment("Radius (in blocks) of the cube searched for leaves around the top of the detected trunk.")
                .defineInRange("leavesSearchRadius", 4, 1, 16);
        builder.pop();

        builder.push("falling");
        FALL_MODE = builder
                .comment("INSTANT: every log above falls the instant the targeted log breaks.",
                        "PROGRESSIVE: logs fall one by one, spaced out over time, for a 'falling tree' effect.")
                .defineEnum("fallMode", FallMode.PROGRESSIVE);
        PROGRESSIVE_DELAY_TICKS = builder
                .comment("Delay, in ticks, between each log falling when fallMode is PROGRESSIVE. (20 ticks = 1 second)")
                .defineInRange("progressiveDelayTicks", 2, 1, 100);
        builder.pop();

        builder.push("breakSpeed");
        BASE_TIME_MULTIPLIER = builder
                .comment("Break-time multiplier applied to the targeted log regardless of tree size.",
                        "1.0 = no penalty for a lone log.")
                .defineInRange("baseTimeMultiplier", 1.0, 0.1, 10.0);
        PER_LOG_TIME_MULTIPLIER = builder
                .comment("Extra break-time multiplier added per additional log detected in the tree.",
                        "Example: 0.04 means each extra log adds 4% to the break time.")
                .defineInRange("perLogTimeMultiplier", 0.04, 0.0, 5.0);
        MAX_TIME_MULTIPLIER = builder
                .comment("Hard cap on the total break-time multiplier, however big the tree is.")
                .defineInRange("maxTimeMultiplier", 5.0, 1.0, 50.0);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    private TreeFellingConfig() {
    }
}
