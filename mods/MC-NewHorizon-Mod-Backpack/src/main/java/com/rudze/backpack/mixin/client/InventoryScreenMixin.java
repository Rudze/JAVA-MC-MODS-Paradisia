package com.rudze.backpack.mixin.client;

import com.rudze.backpack.item.BackpackItems;
import com.rudze.backpack.item.BackpackPanelLayout;
import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the backpack panel background behind the slots that {@code InventoryMenuMixin} appends
 * to the player's inventory menu - but only while the player actually has a backpack equipped.
 * Slot rendering itself (items, hover highlight) is handled automatically by the base screen from
 * the menu's slot list; those slots always exist (see InventoryMenuMixin) but refuse item
 * placement and hold nothing when unequipped, so simply not drawing the panel here is enough to
 * make the whole grid visually disappear along with the backpack.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void backpack$renderBackpackPanel(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !hasBackpackEquipped(player)) {
            return;
        }

        AbstractContainerScreenAccessor self = (AbstractContainerScreenAccessor) (Object) this;
        int x = self.backpack$getLeftPos() + BackpackPanelLayout.PANEL_LEFT;
        int y = self.backpack$getTopPos() + BackpackPanelLayout.PANEL_TOP;

        guiGraphics.blit(BackpackPanelLayout.TEXTURE, x, y, 0.0F, 0.0F,
                BackpackPanelLayout.TEXTURE_WIDTH, BackpackPanelLayout.TEXTURE_HEIGHT,
                BackpackPanelLayout.TEXTURE_WIDTH, BackpackPanelLayout.TEXTURE_HEIGHT);
    }

    private static boolean hasBackpackEquipped(Player player) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        return capability != null && capability.isEquipped(BackpackItems.BACKPACK.get());
    }
}
