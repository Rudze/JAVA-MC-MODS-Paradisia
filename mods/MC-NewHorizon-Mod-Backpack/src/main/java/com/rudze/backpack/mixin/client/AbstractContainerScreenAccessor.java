package com.rudze.backpack.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code AbstractContainerScreen#leftPos}/{@code #topPos}, which are protected, so
 * {@code InventoryScreenMixin} can position the backpack panel relative to the already-centered
 * screen without needing to duplicate that centering math itself.
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int backpack$getLeftPos();

    @Accessor("topPos")
    int backpack$getTopPos();
}
