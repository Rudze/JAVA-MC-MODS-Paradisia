package com.rudze.backpack.item;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

/**
 * A {@link Container} view of "whatever backpack this player currently has equipped in their
 * Accessories back slot", used to show backpack slots directly inside the player's own inventory
 * screen. Stateless by design: every call re-resolves the currently equipped backpack stack and
 * reads/writes its {@link DataComponents#CONTAINER} component directly, so there is nothing to
 * keep in sync if the equipped backpack changes (or is removed) while the inventory is open -
 * the very next call just sees the new reality. A single instance of this class is shared by all
 * 27 slots injected by {@code InventoryMenuMixin}.
 */
public final class DynamicBackpackContainer implements Container {

    public static final int SIZE = 36;

    private final Player player;

    public DynamicBackpackContainer(Player player) {
        this.player = player;
    }

    private ItemStack backpackStack() {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) {
            return ItemStack.EMPTY;
        }
        List<SlotEntryReference> equipped = capability.getEquipped(BackpackItems.BACKPACK.get());
        if (equipped.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return equipped.get(0).stack();
    }

    private NonNullList<ItemStack> readItems() {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ItemStack backpack = backpackStack();
        if (!backpack.isEmpty()) {
            backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        }
        return items;
    }

    private void writeItems(NonNullList<ItemStack> items) {
        ItemStack backpack = backpackStack();
        if (!backpack.isEmpty()) {
            backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        }
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return readItems().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return readItems().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        NonNullList<ItemStack> items = readItems();
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            writeItems(items);
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        NonNullList<ItemStack> items = readItems();
        ItemStack result = items.set(slot, ItemStack.EMPTY);
        writeItems(items);
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        NonNullList<ItemStack> items = readItems();
        items.set(slot, stack);
        writeItems(items);
    }

    @Override
    public void setChanged() {
        // No-op: reads/writes above already go straight through to the backing stack.
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        writeItems(NonNullList.withSize(SIZE, ItemStack.EMPTY));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (backpackStack().isEmpty()) {
            return false;
        }
        return !(stack.getItem() instanceof BackpackItem);
    }
}
