package tallestred.numismaticoverhaul.datagen;

import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import tallestred.numismaticoverhaul.loot_stuff.AddItemModifier;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.config.NOConfig;
import tallestred.numismaticoverhaul.init.ItemInit;
import tallestred.numismaticoverhaul.loot_stuff.MoneyBagLootModifier;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends GlobalLootModifierProvider {
    public ModLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, NumismaticOverhaul.MODID);
    }

    @Override
    protected void start() {
        /*for (String structuresWithCoins : NOConfig.INSTANCE.structuresToHaveCoins.get()) {
            ResourceLocation lootLocation = ResourceLocation.withDefaultNamespace(structuresWithCoins);
            String structureName = lootLocation.getPath().replace("/", "_");
            this.add("generic_coin_loot_table_" + structureName, new AddItemModifier(new LootItemCondition[]{
                    LootItemRandomChanceCondition.randomChance(0.10F).build(),
                    new LootTableIdCondition.Builder(lootLocation).build()}, ItemInit.GOLD_COIN.get()) {
            });
        }
        this.add("money_bag_loot_table_pyramid", new MoneyBagLootModifier(new LootItemCondition[]{
                LootItemRandomChanceCondition.randomChance(0.45F).build(),
                new LootTableIdCondition.Builder(BuiltInLootTables.DESERT_PYRAMID.location()).build()}, 300, 1200) {
        });
        for (ResourceLocation dungeonTypeLoot : List.of(BuiltInLootTables.SIMPLE_DUNGEON.location(), BuiltInLootTables.ABANDONED_MINESHAFT.location())) {
            this.add("money_bag_loot_table_" + dungeonTypeLoot.getPath().replace("/", "_"), new MoneyBagLootModifier(new LootItemCondition[]{
                    LootItemRandomChanceCondition.randomChance(0.75F).build(),
                    new LootTableIdCondition.Builder(dungeonTypeLoot).build()}, 500, 2000) {
            });
        }
        for (ResourceLocation specialTypeLoot : List.of(BuiltInLootTables.BASTION_TREASURE.location(), BuiltInLootTables.STRONGHOLD_CORRIDOR.location(), BuiltInLootTables.PILLAGER_OUTPOST.location(), BuiltInLootTables.BURIED_TREASURE.location())) {
            this.add("money_bag_loot_table_" + specialTypeLoot.getPath().replace("/", "_"), new MoneyBagLootModifier(new LootItemCondition[]{
                    LootItemRandomChanceCondition.randomChance(0.75F).build(),
                    new LootTableIdCondition.Builder(specialTypeLoot).build()}, 1500, 4000) {
            });
        }
        this.add("money_bag_loot_table_stronghold_library", new MoneyBagLootModifier(new LootItemCondition[]{
                LootItemRandomChanceCondition.randomChance(0.85F).build(),
                new LootTableIdCondition.Builder(BuiltInLootTables.STRONGHOLD_LIBRARY.location()).build()}, 2000, 6000) {
        });*/
    }
}
