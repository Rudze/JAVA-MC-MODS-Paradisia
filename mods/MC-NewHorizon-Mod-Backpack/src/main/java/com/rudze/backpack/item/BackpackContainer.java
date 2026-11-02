package com.rudze.backpack.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * A {@link SimpleContainer} backed by a backpack {@link ItemStack}'s {@link DataComponents#CONTAINER}
 * component, following the exact pattern documented by NeoForge for ItemStack-backed containers.
 * Every mutation is immediately written back into the source stack.
 */
public final class BackpackContainer extends SimpleContainer {

    private final ItemStack stack;
    private final Item backpackItem;

    public BackpackContainer(ItemStack stack, int size) {
        super(size);
        this.stack = stack;
        this.backpackItem = stack.getItem();
        stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack candidate) {
        return candidate.getItem() != this.backpackItem;
    }
}
