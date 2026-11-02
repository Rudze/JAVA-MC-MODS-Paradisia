package tallestred.numismaticoverhaul.block;


import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import tallestred.numismaticoverhaul.currency.CurrencyConverter;
import tallestred.numismaticoverhaul.init.ItemComponentInit;
import tallestred.numismaticoverhaul.item.MoneyBagItem;
import tallestred.numismaticoverhaul.item.data_components.MoneyBagComponent;

public record ShopOffer(ItemStack sell, long price) {
    public static final Endec<ShopOffer> ENDEC = StructEndecBuilder.of(
            CodecUtils.toEndec(ItemStack.CODEC).fieldOf("sell", ShopOffer::getSellStack),
            Endec.LONG.fieldOf("price", ShopOffer::getPrice),
            ShopOffer::new
    );


    public ShopOffer {
        if (sell.isEmpty()) throw new IllegalArgumentException("Sell Stack must not be empty");
        if (price == 0) throw new IllegalArgumentException("Price must not be null");
    }

    @SuppressWarnings("ConstantConditions")
    public MerchantOffer toTradeOffer(ShopBlockEntity shop, boolean inexhaustible) {
        boolean isPocketChange = CurrencyConverter.getRequiredCurrencyTypes(price) == 1;
        var buyStack = isPocketChange ? CurrencyConverter.getAsItemStackList(price).getFirst() : MoneyBagItem.fromRawValue(price);
        int maxUses = inexhaustible ? Integer.MAX_VALUE : count(shop.getItems(), sell) / sell.getCount();
        var tradedItem = isPocketChange ? new ItemCost(buyStack.getItem(), buyStack.getCount()) : new ItemCost(BuiltInRegistries.ITEM.wrapAsHolder(buyStack.getItem()), 1, DataComponentPredicate.allOf(DataComponentMap.composite(DataComponentMap.EMPTY, DataComponentMap.builder().set(ItemComponentInit.MONEY_BAG_COMPONENT, MoneyBagComponent.of(price)).build())), buyStack);

        return new MerchantOffer(tradedItem, sell, maxUses, 0, 0);
    }

    public long getPrice() {
        return price;
    }

    public ItemStack getSellStack() {
        return sell.copy();
    }

    public static int count(NonNullList<ItemStack> stacks, ItemStack testStack) {
        int count = 0;
        for (var stack : stacks) {
            if (!ItemStack.isSameItemSameComponents(stack, testStack)) continue;
            count += stack.getCount();
        }
        return count;
    }

    public static int remove(NonNullList<ItemStack> stacks, ItemStack removeStack) {
        int toRemove = removeStack.getCount();
        for (var stack : stacks) {
            if (!ItemStack.isSameItemSameComponents(stack, removeStack)) continue;

            int removed = stack.getCount();
            stack.shrink(toRemove);

            toRemove -= removed;
            if (toRemove < 1) break;
        }
        return removeStack.getCount() - toRemove;
    }

    @Override
    public String toString() {
        return this.sell + "@" + this.price + "coins";
    }
}