package com.rudze.backpack.item;

import com.rudze.backpack.config.BackpackConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Opens a backpack's storage as a plain chest-like menu, whether the stack is held in hand
 * or currently worn in an Accessories slot.
 */
public final class BackpackOpener {

    private BackpackOpener() {
    }

    public static void open(Player player, ItemStack backpackStack) {
        if (!(backpackStack.getItem() instanceof BackpackItem)) {
            return;
        }
        int rows = Mth.clamp(BackpackConfig.ROWS.get(), 1, 6);
        MenuType<ChestMenu> menuType = rowsToMenuType(rows);

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, menuPlayer) -> new ChestMenu(
                        menuType, containerId, playerInventory, new BackpackContainer(backpackStack, rows * 9), rows),
                backpackStack.getHoverName()
        ));
    }

    private static MenuType<ChestMenu> rowsToMenuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }
}
