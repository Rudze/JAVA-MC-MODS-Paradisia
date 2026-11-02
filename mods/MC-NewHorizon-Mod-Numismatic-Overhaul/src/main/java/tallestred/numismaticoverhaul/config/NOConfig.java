package tallestred.numismaticoverhaul.config;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class NOConfig {

    public static final ModConfigSpec CONFIG_SPEC;
    public static final NOConfig INSTANCE;

    static {
        Pair<NOConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(NOConfig::new);
        CONFIG_SPEC = pair.getRight();
        INSTANCE = pair.getLeft();
    }

    public ModConfigSpec.DoubleValue moneyDropChance;
    public ModConfigSpec.DoubleValue chanceForTaggedToDropGoldCoins;
    public ModConfigSpec.IntValue minGold;
    public ModConfigSpec.IntValue maxGold;
    public ModConfigSpec.DoubleValue chanceForTaggedToDropSilverCoins;
    public ModConfigSpec.IntValue minSilver;
    public ModConfigSpec.IntValue maxSilver;
    public ModConfigSpec.DoubleValue chanceForTaggedToDropBronzeCoins;
    public ModConfigSpec.IntValue minBronze;
    public ModConfigSpec.IntValue maxBronze;
    public ModConfigSpec.ConfigValue<List<? extends String>> structuresToHaveCoins;
    public ModConfigSpec.BooleanValue enableVillagerTrading;


    private NOConfig(ModConfigSpec.Builder builder) {
        enableVillagerTrading = builder.comment("Whether villagers/wandering traders trade for the mod's currency instead of vanilla emeralds. false = trades stay 100% vanilla.").define("Enable villager currency trades", false);
        moneyDropChance = builder.defineInRange("% chance for player to drop money", 10.0, -1000, 1000);
        structuresToHaveCoins = builder.comment("Structures that have coins, specific rates can be changed via datapacks.").defineListAllowEmpty("Structures",
                ImmutableList.of(BuiltInLootTables.STRONGHOLD_LIBRARY.location().toString().toString().toString().toString(), BuiltInLootTables.BASTION_TREASURE.location().toString().toString(), BuiltInLootTables.STRONGHOLD_CORRIDOR.location().toString().toString(),
                        BuiltInLootTables.PILLAGER_OUTPOST.location().toString().toString(), BuiltInLootTables.BURIED_TREASURE.location().toString().toString(), BuiltInLootTables.SIMPLE_DUNGEON.location().toString().toString(), BuiltInLootTables.ABANDONED_MINESHAFT.location().toString().toString()), obj -> true);
        chanceForTaggedToDropBronzeCoins = builder.comment("% chance for mobs tagged bourgeoisie to drop bronze coins").defineInRange("% chance bronze coins", 50.0, -1000, 1000);
        minBronze = builder.defineInRange("Min amount of Bronze Coins to be dropped", 9, -1000, 1000);
        maxBronze = builder.defineInRange("Max amount of Bronze Coins to be dropped", 35, -1000, 1000);
        chanceForTaggedToDropSilverCoins = builder.comment("% chance for mobs tagged bourgeoisie to drop silver coins").defineInRange("% chance silver coins", 80.0, -1000, 1000);
        minSilver = builder.defineInRange("Min amount of Silver Coins to be dropped", 1, -1000, 1000);
        maxSilver = builder.defineInRange("Max amount of Silver Coins to be dropped", 1, -1000, 1000);
        chanceForTaggedToDropGoldCoins = builder.comment("% chance for mobs tagged bourgeoisie to drop gold coins").defineInRange("% chance gold coins", 0.0, -1000, 1000);
        minGold = builder.defineInRange("Min amount of Gold Coins to be dropped", 0, -1000, 1000);
        maxGold = builder.defineInRange("Max amount of Gold Coins to be dropped", 0, -1000, 1000);
    }
}