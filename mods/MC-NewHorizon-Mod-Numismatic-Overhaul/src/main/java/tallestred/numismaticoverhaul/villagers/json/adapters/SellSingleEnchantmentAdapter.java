package tallestred.numismaticoverhaul.villagers.json.adapters;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.trading.ItemCost;
import tallestred.numismaticoverhaul.currency.CurrencyHelper;
import tallestred.numismaticoverhaul.villagers.json.TradeJsonAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SellSingleEnchantmentAdapter extends TradeJsonAdapter {

    @Override
    @NotNull
    public VillagerTrades.ItemListing deserialize(JsonObject json) {
        loadDefaultStats(json, false);
        return new Factory(max_uses, villager_experience, price_multiplier);
    }

    private static class Factory implements VillagerTrades.ItemListing, NumOTrade {
        private final int experience;
        private final int maxUses;
        private final float multiplier;

        public Factory(int maxUses, int experience, float multiplier) {
            this.experience = experience;
            this.maxUses = maxUses;
            this.multiplier = multiplier;
        }

        public MerchantOffer getOffer(Entity entity, RandomSource random) {
            int cost;
            ItemStack itemStack;

            var optionalEnchantment = entity.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getRandomElementOf(EnchantmentTags.TRADEABLE, random);
            if (optionalEnchantment.isPresent()) {
                var enchantmentEntry = optionalEnchantment.get();
                var enchantment = enchantmentEntry.value();

                var enchantmentLevel = Mth.nextInt(random, enchantment.getMinLevel(), enchantment.getMaxLevel());
                itemStack = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantmentEntry, enchantmentLevel));

                cost = 100 * (10 / enchantment.getWeight()) + (random.nextInt(50) + enchantmentLevel) * enchantmentLevel * enchantmentLevel * (10 / enchantment.getWeight());
                if (enchantmentEntry.is(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
                    cost *= 2;
                }
            } else {
                cost = 1;
                itemStack = new ItemStack(Items.BOOK);
            }

            var itemAndCost = CurrencyHelper.getClosest(cost);

            return new MerchantOffer(new ItemCost(itemAndCost.getItem(), itemAndCost.getCount()), Optional.of(new ItemCost(Items.BOOK)), itemStack, maxUses, this.experience, multiplier);
        }
    }
}
