package tallestred.numismaticoverhaul.item;

import net.minecraft.world.item.ItemStack;

public interface CurrencyItem {
    boolean wasAdjusted(ItemStack other);

    long getValue(ItemStack stack);

    long[] getCombinedValue(ItemStack stack);

}
