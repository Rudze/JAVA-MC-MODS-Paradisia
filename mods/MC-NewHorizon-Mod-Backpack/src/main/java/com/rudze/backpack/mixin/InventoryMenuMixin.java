package com.rudze.backpack.mixin;

import com.rudze.backpack.item.BackpackPanelLayout;
import com.rudze.backpack.item.DynamicBackpackContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Appends the player's equipped backpack's slots directly to their own (always-present) inventory
 * menu, so they show up next to the normal inventory grid without needing a separate screen. The
 * slot count is fixed regardless of what's currently equipped (or whether anything is) -
 * {@link DynamicBackpackContainer} resolves "what backpack, if any" on every access, so the slots
 * simply refuse item placement when nothing is equipped rather than needing to be added or removed
 * at runtime (which would shift every other slot's index and risk desyncing client/server).
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void backpack$addBackpackSlots(Inventory inventory, boolean onServer, Player owner, CallbackInfo ci) {
        DynamicBackpackContainer backpack = new DynamicBackpackContainer(owner);
        AbstractContainerMenuAccessor self = (AbstractContainerMenuAccessor) (Object) this;
        for (int i = 0; i < DynamicBackpackContainer.SIZE; i++) {
            self.backpack$addSlot(new Slot(backpack, i, BackpackPanelLayout.slotX(i), BackpackPanelLayout.slotY(i)));
        }
    }
}
