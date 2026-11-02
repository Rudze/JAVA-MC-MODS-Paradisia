package com.rudze.backpack.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@code AbstractContainerMenu#addSlot}, which is protected, so slots can be appended to
 * a menu (like the always-present {@code InventoryMenu}) from outside its own class hierarchy.
 * The Accessor/Invoker pattern targets the class that actually declares the member directly,
 * unlike {@code @Shadow} on a mixin targeting a subclass, which cannot see inherited members.
 */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {

    @Invoker("addSlot")
    Slot backpack$addSlot(Slot slot);
}
