package tallestred.numismaticoverhaul.villagers.data;

import net.minecraft.world.item.trading.ItemCost;
import tallestred.numismaticoverhaul.currency.CurrencyHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

public class RemappingTradeWrapper implements VillagerTrades.ItemListing {

    private final VillagerTrades.ItemListing delegate;

    private RemappingTradeWrapper(VillagerTrades.ItemListing delegate) {
        this.delegate = delegate;
    }

    public static RemappingTradeWrapper wrap(VillagerTrades.ItemListing delegate) {
        return new RemappingTradeWrapper(delegate);
    }

    @Nullable
    @Override
    public MerchantOffer getOffer(Entity entity, RandomSource random) {
        final var tempOffer = delegate.getOffer(entity, random);

        if (tempOffer == null) return null;

        final var firstBuyRemapped = remap(tempOffer.getBaseCostA());
        final var secondBuyRemapped = tempOffer.getItemCostB().map(RemappingTradeWrapper::remap);
        final var sellRemapped = remap(tempOffer.getResult());

        return new MerchantOffer(firstBuyRemapped, secondBuyRemapped, sellRemapped.itemStack(), tempOffer.getUses(), tempOffer.getMaxUses(), tempOffer.getXp(), tempOffer.getPriceMultiplier(), tempOffer.getDemand());
    }

    private static ItemCost remap(ItemCost tradedItem) {
        if (!tradedItem.itemStack().is(Items.EMERALD)) {
            return tradedItem;
        }

        return CurrencyHelper.getClosestTradeItem(convertEmeraldsToCoins(tradedItem.count()));
    }

    private static ItemCost remap(ItemStack stack) {
        if (stack.getItem() != Items.EMERALD) return new ItemCost(stack.getItem(), stack.getCount());

        final int moneyWorth = stack.getCount() * 125;

        return CurrencyHelper.getClosestTradeItem(convertEmeraldsToCoins(moneyWorth));
    }

    private static long convertEmeraldsToCoins(int stackCount) {
        return (long) stackCount * 125;
    }
}
