package tallestred.numismaticoverhaul.item;

import tallestred.numismaticoverhaul.cap.CurrencyHolder;
import tallestred.numismaticoverhaul.currency.CurrencyConverter;
import tallestred.numismaticoverhaul.currency.CurrencyHelper;
import tallestred.numismaticoverhaul.currency.CurrencyResolver;
import tallestred.numismaticoverhaul.init.ItemComponentInit;
import tallestred.numismaticoverhaul.init.ItemInit;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tallestred.numismaticoverhaul.item.data_components.MoneyBagComponent;

import java.util.Optional;

import static tallestred.numismaticoverhaul.init.ItemComponentInit.MONEY_BAG_COMPONENT;

public class MoneyBagItem extends Item implements CurrencyItem {

    public MoneyBagItem(Item.Properties properties) {
        super(properties);
    }


    public static ItemStack create(ItemStack firstStack, ItemStack otherStack) {
        var stack = new ItemStack(ItemInit.MONEY_BAG.get());
        if (firstStack.has(MONEY_BAG_COMPONENT) && otherStack.has(MONEY_BAG_COMPONENT)) {
            stack.set(MONEY_BAG_COMPONENT, MoneyBagComponent.combine(firstStack, otherStack));
        } else if (firstStack.getItem() instanceof CurrencyItem coins && otherStack.getItem() instanceof CurrencyItem coins2) {
            var values1 = coins.getCombinedValue(firstStack);
            var values2 = coins2.getCombinedValue(otherStack);
            stack.set(MONEY_BAG_COMPONENT, MoneyBagComponent.combine(values1, values2));
        }
        return stack;
    }

    public static ItemStack fromValues(long[] values) {
        var stack = new ItemStack(ItemInit.MONEY_BAG.get());
        stack.set(MONEY_BAG_COMPONENT, MoneyBagComponent.of(values)
        );
        return stack;
    }

    public static ItemStack fromRawValue(long value) {
        var stack = new ItemStack(ItemInit.MONEY_BAG.get());
        stack.set(MONEY_BAG_COMPONENT, MoneyBagComponent.of(value));
        return stack;
    }

    public long getValue(ItemStack stack) {
        return stack.getOrDefault(MONEY_BAG_COMPONENT, MoneyBagComponent.of(0)).value();
    }

    @Override
    public long[] getCombinedValue(ItemStack stack) {
        var bagComponent = stack.getOrDefault(MONEY_BAG_COMPONENT, MoneyBagComponent.of(0));
        return new long[]{bagComponent.bronze(), bagComponent.silver(), bagComponent.gold()};
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack clickedStack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player, SlotAccess cursorStackReference) {
        if (slot instanceof MerchantResultSlot) return false;

        if (clickType == ClickAction.SECONDARY && clickedStack.getItem() == this && otherStack.isEmpty()) {
            var coins = getCombinedValue(clickedStack);
            final var stackRepresentation = CurrencyConverter.getAsValidStacks(coins);
            if (stackRepresentation.isEmpty()) return false;

            final var coinStack = stackRepresentation.getFirst();
            cursorStackReference.set(coinStack);

            final long[] values = getCombinedValue(clickedStack);
            values[((CoinItem) coinStack.getItem()).currency.ordinal()] -= coinStack.getCount();

            final long newValue = CurrencyResolver.combineValues(values);
            final boolean canBeCompacted = CurrencyResolver.canBeCompacted(values);

            if (newValue == 0) {
                slot.set(ItemStack.EMPTY);
            } else if (canBeCompacted && CurrencyConverter.getAsValidStacks(newValue).size() == 1) {
                slot.set(CurrencyConverter.getAsValidStacks(newValue).getFirst());
            } else {
                slot.set(fromValues(values));
            }

        } else if (clickType == ClickAction.PRIMARY) {
            if (!(otherStack.getItem() instanceof CurrencyItem currencyItem)) return false;
            final var bag = MoneyBagItem.create(clickedStack, otherStack);
            if (bag.getOrDefault(MONEY_BAG_COMPONENT, MoneyBagComponent.of(0)).value() == 0) return false;
            if (!slot.mayPlace(bag)) return false;

            slot.set(bag);
            return cursorStackReference.set(ItemStack.EMPTY);
        }

        return true;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var values = this.getCombinedValue(stack);
        return Optional.of(new CurrencyTooltipData(values, new long[]{-1}));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        CurrencyHolder.modify(user, getValue(user.getItemInHand(hand)));
        user.setItemInHand(hand, ItemStack.EMPTY);
        return InteractionResultHolder.success(ItemStack.EMPTY);
    }

    @Override
    public boolean wasAdjusted(ItemStack other) {
        return true;
    }

    @Override
    public Component getDescription() {
        return super.getDescription().copy().setStyle(((CoinItem)ItemInit.SILVER_COIN.get()).NAME_STYLE);
    }

}
