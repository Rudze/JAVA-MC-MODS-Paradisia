package com.rudze.quests.tax;

import com.rudze.worldbuild.claim.Coins;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import tallestred.numismaticoverhaul.item.CurrencyItem;

import java.util.List;

/**
 * The tax chest: a plain 3-row chest as far as the client knows (vanilla {@link MenuType#GENERIC_9x3}),
 * so it works without the mod client-side. The server only accepts Numismatic coins and money
 * bags, and keeps a paper in the bottom-middle slot showing what is owed and what has been put in.
 *
 * <p>Nothing is paid until the chest is closed: until then the player can take coins back out.
 */
public class TaxMenu extends AbstractContainerMenu {

    private static final int ROWS = 3;
    private static final int SLOTS = ROWS * 9;
    private static final int INFO_SLOT = 22;

    private final SimpleContainer coins = new SimpleContainer(SLOTS);
    private final SimpleContainer info = new SimpleContainer(1);
    private final long owed;

    public TaxMenu(int containerId, Inventory playerInventory, long owed) {
        super(MenuType.GENERIC_9x3, containerId);
        this.owed = owed;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                addSlot(index == INFO_SLOT ? new InfoSlot(info, x, y) : new CoinSlot(coins, index, x, y));
            }
        }
        int playerY = (ROWS - 4) * 18 + 103;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, playerY + 58));
        }

        coins.addListener(container -> refreshInfo());
        refreshInfo();
    }

    private long deposited() {
        long total = 0;
        for (int i = 0; i < coins.getContainerSize(); i++) {
            ItemStack stack = coins.getItem(i);
            if (stack.getItem() instanceof CurrencyItem currency) {
                total += currency.getValue(stack);
            }
        }
        return total;
    }

    private void refreshInfo() {
        long deposited = deposited();
        long after = owed - deposited;
        ItemStack paper = new ItemStack(Items.PAPER);
        paper.set(DataComponents.CUSTOM_NAME, plain("Impôts fonciers", ChatFormatting.GOLD));
        paper.set(DataComponents.LORE, new ItemLore(List.of(
                plain("Dû : " + Coins.format(Math.max(owed, 0)), ChatFormatting.RED),
                plain("Déposé : " + Coins.format(deposited), ChatFormatting.GREEN),
                after > 0 ? plain("Reste à payer : " + Coins.format(after), ChatFormatting.YELLOW)
                        : plain("Avance : " + Coins.format(-after), ChatFormatting.AQUA),
                plain("Pose tes pièces puis ferme le coffre.", ChatFormatting.GRAY))));
        info.setItem(0, paper);
    }

    private static Component plain(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(style -> style.withColor(color).withItalic(false));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        long amount = deposited();
        for (int i = 0; i < coins.getContainerSize(); i++) {
            ItemStack stack = coins.removeItemNoUpdate(i);
            // Can't normally happen (CoinSlot filters), but never swallow a non-coin item.
            if (!stack.isEmpty() && !(stack.getItem() instanceof CurrencyItem)) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
        if (amount > 0) {
            TaxOffice.pay(serverPlayer, amount);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < SLOTS
                ? moveItemStackTo(stack, SLOTS, slots.size(), true)
                : moveItemStackTo(stack, 0, SLOTS, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    private static final class CoinSlot extends Slot {
        CoinSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof CurrencyItem;
        }
    }

    private static final class InfoSlot extends Slot {
        InfoSlot(Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
