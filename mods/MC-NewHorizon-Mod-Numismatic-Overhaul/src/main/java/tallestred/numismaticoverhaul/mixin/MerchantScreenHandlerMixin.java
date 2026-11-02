package tallestred.numismaticoverhaul.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.trading.ItemCost;
import tallestred.numismaticoverhaul.cap.CurrencyHolder;
import tallestred.numismaticoverhaul.currency.CurrencyHelper;
import tallestred.numismaticoverhaul.entity.WheatMerchantEntity;
import tallestred.numismaticoverhaul.init.DataAttachmentInit;
import tallestred.numismaticoverhaul.init.ItemInit;
import tallestred.numismaticoverhaul.item.CoinItem;
import tallestred.numismaticoverhaul.item.MoneyBagItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantMenu.class)
public class MerchantScreenHandlerMixin {

    @Shadow
    @Final
    private Merchant trader;

    /**
     * Shift-clicking the result slot goes through {@code quickMoveStack}, not {@code MerchantResultSlot#onTake}
     * (see {@code MerchantResultSlotMixin}) - and critically, {@code quickMoveStack} calls
     * {@code moveItemStackTo(...)} (which is what actually copies the item into the player's
     * inventory slots) *before* it calls {@code onTake}. So stripping the "Stock: N" lore in
     * {@code onTake} alone fixes a normal click-and-drag take, but is too late for shift-click - the
     * lore-carrying copies are already sitting in the player's inventory by then. Stripped here
     * instead, on the slot's actual item, before {@code quickMoveStack} runs its own logic and reads it.
     */
    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void numismatic$stripStockLoreOnQuickMove(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (slotIndex == 2 && trader instanceof WheatMerchantEntity) {
            MerchantMenu menu = (MerchantMenu) (Object) this;
            menu.getSlot(2).getItem().remove(DataComponents.LORE);
        }
    }

    @Redirect(method = "moveFromInventoryToPaymentSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    public boolean isSameItemSameTags(ItemStack stack1, ItemStack stack2) {
        if (stack1.getItem() instanceof CoinItem) {
            return stack1.getItem() == stack2.getItem();
        }
        return ItemStack.isSameItemSameComponents(stack1, stack2);
    }

    //Autofill with coins from the player's purse if the trade requires it
    //Injected at TAIL to let normal autofill run and fill up if anything is missing
    @Inject(method = "moveFromInventoryToPaymentSlot", at = @At("TAIL"))
    public void autofillOverride(int slot, ItemCost payment, CallbackInfo ci) {
        MerchantMenu handler = (MerchantMenu) (Object) this;
        Player player = ((Inventory) handler.getSlot(3).container).player;
        ItemStack stack = payment.itemStack();
        if (stack.getItem() instanceof CoinItem) {
            numismatic$autofillWithCoins(slot, stack, handler);
        } else if (stack.getItem() == ItemInit.MONEY_BAG.get()) {
            autofillWithMoneyBag(slot, stack, handler);
        }
    }

    private static void numismatic$autofillWithCoins(int slot, ItemStack stack, MerchantMenu handler) {
        //See how much is required and how much was already autofilled
        long requiredCurrency = ((CoinItem) stack.getItem()).currency.getRawValue(stack.getCount());
        long presentCurrency = ((CoinItem) stack.getItem()).currency.getRawValue(handler.getSlot(slot).getItem().getCount());

        if (requiredCurrency <= presentCurrency) return;

        //Find out how much we still need to fill
        long neededCurrency = requiredCurrency - presentCurrency;

        //Is that even possible?
        Player player = ((Inventory) handler.getSlot(3).container).player;
        if (!(neededCurrency <= CurrencyHolder.getValue(player))) return;
        CurrencyHolder.silentModify(player, -neededCurrency);
        handler.slots.get(slot).set(stack.copy());
    }

    private static void autofillWithMoneyBag(int slot, ItemStack stack, MerchantMenu handler) {
        if (ItemStack.isSameItemSameComponents(stack, handler.getSlot(slot).getItem())) return;
        Player player = ((Inventory) handler.getSlot(3).container).player;

        //See how much is required and how much in present in the player's inventory
        long requiredCurrency = ((MoneyBagItem) ItemInit.MONEY_BAG.get()).getValue(stack);
        long availableCurrencyInPlayerInventory = CurrencyHelper.getMoneyInInventory(player, false);

        //Find out how much we still need to fill
        long neededCurrency = requiredCurrency - availableCurrencyInPlayerInventory;

        //Is that even possible?
        if (neededCurrency > CurrencyHolder.getValue(player)) return;

        if (neededCurrency <= 0) {
            CurrencyHelper.deduceFromInventory(player, requiredCurrency);
        } else {
            CurrencyHelper.deduceFromInventory(player, availableCurrencyInPlayerInventory);
        }

        handler.slots.get(slot).set(stack.copy());
    }

    @Inject(method = "playTradeSound", at = @At("HEAD"), cancellable = true)
    public void checkForEntityOnYes(CallbackInfo ci) {
        if (!(trader instanceof Entity)) ci.cancel();
    }

}
