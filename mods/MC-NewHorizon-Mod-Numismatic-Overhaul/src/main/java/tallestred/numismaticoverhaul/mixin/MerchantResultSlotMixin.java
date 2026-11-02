package tallestred.numismaticoverhaul.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tallestred.numismaticoverhaul.entity.WheatMerchantEntity;

/**
 * {@code WheatMerchantEntity#updateTrades} bakes a live "Stock: N" lore line onto the wheat
 * {@link ItemStack} shown in the trade screen, so the stock is visible while shopping (see that
 * class). But {@code MerchantOffer#assemble()} just copies that exact stack - lore included - into
 * whatever the player actually walks away with, so without this the lore permanently sticks to
 * ordinary wheat sitting in the player's inventory. Stripped here, right as the item leaves the
 * result slot - the earliest point with a direct reference to the exact stack the player is about
 * to receive, before it's merged into their inventory.
 */
@Mixin(MerchantResultSlot.class)
public class MerchantResultSlotMixin {

    @Shadow
    @Final
    private Merchant merchant;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void numismatic$stripStockLore(Player player, ItemStack stack, CallbackInfo ci) {
        if (merchant instanceof WheatMerchantEntity) {
            stack.remove(DataComponents.LORE);
        }
    }
}
