package tallestred.numismaticoverhaul.villagers.json.adapters;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.trading.ItemCost;
import tallestred.numismaticoverhaul.currency.CurrencyHelper;
import tallestred.numismaticoverhaul.villagers.json.TradeJsonAdapter;
import tallestred.numismaticoverhaul.villagers.json.VillagerJsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class SellPotionContainerItemAdapter extends TradeJsonAdapter {

    @Override
    @NotNull
    public VillagerTrades.ItemListing deserialize(JsonObject json) {

        loadDefaultStats(json, true);

        VillagerJsonHelper.assertJsonObject(json, "container_item");
        VillagerJsonHelper.assertJsonObject(json, "buy_item");

        int price = json.get("price").getAsInt();
        ItemStack container_item = VillagerJsonHelper.getItemStackFromJson(json.get("container_item").getAsJsonObject());
        ItemStack buy_item = VillagerJsonHelper.getItemStackFromJson(json.get("buy_item").getAsJsonObject());

        return new Factory(container_item, buy_item, price, max_uses, villager_experience, price_multiplier);
    }

    private static class Factory implements VillagerTrades.ItemListing, NumOTrade {
        private final ItemStack containerItem;
        private final ItemStack buyItem;

        private final int price;
        private final int maxUses;
        private final int experience;

        private final float priceMultiplier;

        public Factory(ItemStack containerItem, ItemStack buyItem, int price, int maxUses, int experience, float priceMultiplier) {
            this.containerItem = containerItem;
            this.buyItem = buyItem;
            this.price = price;
            this.maxUses = maxUses;
            this.experience = experience;
            this.priceMultiplier = priceMultiplier;
        }

        public MerchantOffer getOffer(Entity entity, RandomSource random) {
            List<Potion> list = BuiltInRegistries.POTION.stream().filter((potion) -> {
                return !potion.getEffects().isEmpty() && entity.level().potionBrewing().isBrewablePotion(BuiltInRegistries.POTION.wrapAsHolder(potion));
            }).toList();
            Potion potion = list.get(random.nextInt(list.size()));
            ItemStack itemStack2 = PotionContents.createItemStack(containerItem.getItem(), BuiltInRegistries.POTION.wrapAsHolder(potion));
            return new MerchantOffer(CurrencyHelper.getClosestTradeItem(price), Optional.of(new ItemCost(buyItem.getItem(), buyItem.getCount())), itemStack2, this.maxUses, this.experience, this.priceMultiplier);
        }
    }
}
