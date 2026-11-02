package tallestred.numismaticoverhaul.villagers.json.adapters;

import com.google.gson.JsonObject;
import tallestred.numismaticoverhaul.currency.CurrencyHelper;
import tallestred.numismaticoverhaul.villagers.json.TradeJsonAdapter;
import tallestred.numismaticoverhaul.villagers.json.VillagerJsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.NotNull;

public class SellStackAdapter extends TradeJsonAdapter {

    @Override
    @NotNull
    public VillagerTrades.ItemListing deserialize(JsonObject json) {

        loadDefaultStats(json, true);

        VillagerJsonHelper.assertJsonObject(json, "sell");

        ItemStack sell = VillagerJsonHelper.getItemStackFromJson(json.get("sell").getAsJsonObject());
        int price = json.get("price").getAsInt();

        return new Factory(sell, price, max_uses, villager_experience, price_multiplier);
    }

    private static class Factory implements VillagerTrades.ItemListing, NumOTrade {
        private final ItemStack sell;
        private final int maxUses;
        private final int experience;
        private final int price;
        private final float multiplier;

        public Factory(ItemStack sell, int price, int maxUses, int experience, float multiplier) {
            this.sell = sell;
            this.maxUses = maxUses;
            this.experience = experience;
            this.price = price;
            this.multiplier = multiplier;
        }

        public MerchantOffer getOffer(Entity entity, RandomSource random) {
            return new MerchantOffer(CurrencyHelper.getClosestTradeItem(price), sell, this.maxUses, this.experience, multiplier);
        }
    }
}
