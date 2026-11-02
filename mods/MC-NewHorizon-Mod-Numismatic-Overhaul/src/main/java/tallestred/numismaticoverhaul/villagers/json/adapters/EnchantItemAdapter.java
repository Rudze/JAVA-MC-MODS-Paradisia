package tallestred.numismaticoverhaul.villagers.json.adapters;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.ItemCost;
import tallestred.numismaticoverhaul.currency.CurrencyHelper;
import tallestred.numismaticoverhaul.villagers.json.TradeJsonAdapter;
import tallestred.numismaticoverhaul.villagers.json.VillagerJsonHelper;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EnchantItemAdapter extends TradeJsonAdapter {

    @Override
    @NotNull
    public VillagerTrades.ItemListing deserialize(JsonObject json) {

        loadDefaultStats(json, false);
        VillagerJsonHelper.assertInt(json, "level");

        boolean allow_treasure = VillagerJsonHelper.boolean_getOrDefault(json, "allow_treasure", false);

        int level = json.get("level").getAsInt();
        ItemStack item = VillagerJsonHelper.ItemStack_getOrDefault(json, "item", new ItemStack(Items.BOOK));
        int base_price = GsonHelper.getAsInt(json, "base_price", 200);

        return new Factory(item, max_uses, villager_experience, level, allow_treasure, price_multiplier, base_price);
    }

    private static class Factory implements VillagerTrades.ItemListing, NumOTrade {
        private final int experience;
        private final int maxUses;
        private final int level;
        private final boolean allowTreasure;
        private final ItemStack toEnchant;
        private final float multiplier;
        private final int basePrice;

        public Factory(ItemStack item, int maxUses, int experience, int level, boolean allowTreasure, float multiplier, int basePrice) {
            this.experience = experience;
            this.maxUses = maxUses;
            this.level = level;
            this.allowTreasure = allowTreasure;
            this.toEnchant = item;
            this.multiplier = multiplier;
            this.basePrice = basePrice;
        }

        public MerchantOffer getOffer(Entity entity, RandomSource random) {
            var itemStack = toEnchant.copy();

            var enchantmentRegistry = entity.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            var nonTreasureEnchants = enchantmentRegistry.getTag(EnchantmentTags.NON_TREASURE);
            var treasureRegistry = enchantmentRegistry.getTag(EnchantmentTags.TRADEABLE);
            var enchants = List.<EnchantmentInstance>of();
            if (allowTreasure && treasureRegistry.isPresent()) {
                enchants = EnchantmentHelper.selectEnchantment(random, itemStack, level, treasureRegistry.get().stream());
            }
            else if (nonTreasureEnchants.isPresent()) {
                enchants = EnchantmentHelper.selectEnchantment(random, itemStack, level, nonTreasureEnchants.get().stream());
            }

            var finalItemStack = itemStack.copy();
            if (finalItemStack.is(Items.BOOK)) {
                finalItemStack = new ItemStack(Items.ENCHANTED_BOOK);
            }

            for (EnchantmentInstance enchant : enchants) {
                finalItemStack.enchant(enchant.enchantment, enchant.level);
            }

            int price = basePrice;
            var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(finalItemStack);

            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                var enchantment = entry.getKey();
                var isTreasure = enchantment.is(EnchantmentTags.TREASURE);

                if (enchantment.is(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
                    price *= 2;
                }
                price += (int) (price * 0.10f + basePrice * (isTreasure ? 2f : 1f) *
                        entry.getIntValue() * Mth.nextFloat(random, .8f, 1.2f)
                        * (5f / (float) enchantment.value().getWeight()));
            }

            var itemAndCost = CurrencyHelper.getClosest(price);
            return new MerchantOffer(new ItemCost(itemAndCost.getItem(), itemAndCost.getCount()), Optional.of(new ItemCost(toEnchant.getItem())), finalItemStack, maxUses, this.experience, multiplier);
        }
    }
}
