package tallestred.numismaticoverhaul.economy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import tallestred.numismaticoverhaul.currency.Currency;
import tallestred.numismaticoverhaul.entity.WheatMerchantEntity;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Every crop {@link WheatMerchantEntity} (Thomas Miller) trades, with base prices balanced against
 * each other rather than picked arbitrarily. Wheat's price (5 bronze) was already fixed before this
 * class existed, so it's the calibration anchor everything else is priced relative to.
 *
 * <p>The balancing input for each crop is "how much real farming effort does one unit represent",
 * approximated as {@code growTime / averageYieldPerHarvest} using actual vanilla growth mechanics:
 * <ul>
 *   <li>Wheat, Carrot and Potato all use the exact same 8-stage {@code CropBlock} growth mechanic, so
 *   they take statistically identical time to mature. Wheat's harvest yields exactly 1 Wheat item;
 *   Carrot/Potato yield 1 + a random 0-3 bonus (average 2.5) - so per unit, Carrot/Potato represent
 *   {@code 1 / 2.5 = 0.4x} the effort of Wheat, hence 0.4x the price (2 bronze).</li>
 *   <li>Beetroot only has 4 growth stages (half of Wheat's 8) under the same random-tick mechanic, so
 *   it matures roughly twice as fast, while yielding the same average 2.5 per harvest as Carrot/Potato
 *   - half the time *and* the same yield makes it the cheapest per-unit crop (1 bronze).</li>
 *   <li>Apples aren't a tilled crop at all - they're a rare (~0.5% per random tick) leaf-decay drop
 *   from a fully grown oak tree, and a tree takes far longer to reach maturity than any tilled crop's
 *   growth cycle. Both a much longer cycle and a much lower yield-per-cycle push the price well above
 *   every farmed crop (50 bronze - a 10x premium over Wheat).</li>
 * </ul>
 * These are deliberately approximate, static design inputs for balancing - not a live simulation of
 * Minecraft's random-tick growth RNG.
 *
 * <p>Farmer's Delight crops (optional - see {@link #cropIfPresent}) are balanced the same way, this
 * time from that mod's own loot tables/blockstates rather than vanilla's:
 * <ul>
 *   <li>Tomato: 4 growth stages (half of Wheat's 8), yields 1-2 (average 1.5) - faster *and* slightly
 *   more than Wheat per cycle, timePerUnit {@code 4/1.5 ≈ 2.7} vs Wheat's 8 → ~0.33x → 2 bronze.</li>
 *   <li>Onion: 8 stages (same as Wheat), yields 1 guaranteed + a binomial(3, 57%) bonus at full growth
 *   (average ≈ 2.7, matching vanilla Carrot/Potato) → ~0.37x → 2 bronze, same tier as Tomato.</li>
 *   <li>Cabbage: 8 stages (same as Wheat) but yields exactly 1 Cabbage - the loot table's bonus roll
 *   on that block is extra *seeds*, not extra Cabbages - so its effort-per-unit matches Wheat exactly
 *   → 5 bronze, same as Wheat (Cabbage is deliberately the "slow, single big harvest" crop).</li>
 *   <li>Rice: grows in two sequential 4-stage phases ({@code rice} then {@code rice_panicles}, 8 stages
 *   total) for exactly 1 Rice per cycle - same total time and yield as Wheat → 5 bronze.</li>
 * </ul>
 *
 * <p>All crops share {@link CropMarket#BASE_STOCK}/{@link CropMarket#MAX_STOCK} - only the base price
 * differs - so every commodity has the same price elasticity and only its absolute price level differs.
 */
public final class Crops {
    private Crops() {
    }

    public static final CropConfig WHEAT = crop("wheat", Items.WHEAT, "Blé", 5);
    public static final CropConfig CARROT = crop("carrot", Items.CARROT, "Carotte", 2);
    public static final CropConfig POTATO = crop("potato", Items.POTATO, "Pomme de terre", 2);
    public static final CropConfig BEETROOT = crop("beetroot", Items.BEETROOT, "Betterave", 1);
    public static final CropConfig APPLE = crop("apple", Items.APPLE, "Pomme", 50);

    // Farmer's Delight - optional, see cropIfPresent: silently dropped from ALL if that mod isn't installed.
    public static final CropConfig TOMATO = cropIfPresent("tomato", "farmersdelight:tomato", "Tomate", 2);
    public static final CropConfig ONION = cropIfPresent("onion", "farmersdelight:onion", "Oignon", 2);
    public static final CropConfig CABBAGE = cropIfPresent("cabbage", "farmersdelight:cabbage", "Chou", 5);
    public static final CropConfig RICE = cropIfPresent("rice", "farmersdelight:rice", "Riz", 5);

    public static final List<CropConfig> ALL = Stream.of(WHEAT, CARROT, POTATO, BEETROOT, APPLE, TOMATO, ONION, CABBAGE, RICE)
            .filter(Objects::nonNull)
            .toList();

    private static CropConfig crop(String commodityId, Item item, String displayName, long basePriceBronze) {
        return new CropConfig(
                WheatMerchantEntity.MERCHANT_ID, WheatMerchantEntity.MERCHANT_NAME, WheatMerchantEntity.MERCHANT_PORTRAIT,
                commodityId, item, displayName, Currency.BRONZE.getRawValue(basePriceBronze)
        );
    }

    // Unlike vanilla Items constants (always present at compile time), a modded item id might not
    // exist - Farmer's Delight is a soft dependency, not a compile-time one, so this looks the item
    // up by registry name at runtime and returns null (filtered out of ALL above) rather than crashing
    // when that mod isn't installed.
    private static CropConfig cropIfPresent(String commodityId, String itemId, String displayName, long basePriceBronze) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId)).orElse(null);
        if (item == null) return null;
        return crop(commodityId, item, displayName, basePriceBronze);
    }
}
