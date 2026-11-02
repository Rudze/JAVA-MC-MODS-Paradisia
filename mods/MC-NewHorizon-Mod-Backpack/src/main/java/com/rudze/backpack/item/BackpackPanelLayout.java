package com.rudze.backpack.item;

import net.minecraft.resources.ResourceLocation;

/**
 * Shared layout constants for the backpack panel appended to the player's inventory screen -
 * used by both {@code InventoryMenuMixin} (slot positions, runs on client and server since
 * {@code InventoryMenu} is constructed on both) and the client-only {@code InventoryScreenMixin}
 * (background texture), which must agree on where things sit.
 *
 * <p>Texture and slot-grid positions are copied from a reference mod's own "4x9" backpack panel
 * asset ({@code textures/gui/backpack_panel.png}, 86x166): a 7px horizontal border and an
 * estimated ~2px vertical border around an 18px-per-cell, 4-column x 9-row slot grid. The panel
 * sits flush against the left edge of the vanilla inventory background (no gap), matching that
 * mod's own "backgroundX = -texture width" convention.
 */
public final class BackpackPanelLayout {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("nh_backpack", "textures/gui/backpack_panel.png");

    public static final int TEXTURE_WIDTH = 86;
    public static final int TEXTURE_HEIGHT = 166;

    public static final int PANEL_LEFT = -TEXTURE_WIDTH;
    public static final int PANEL_TOP = 0;

    // Tuned by hand against the actual texture: the grid was 1px too far left and 3px too far
    // down relative to the drawn cells, so nudge +1 X / -3 Y from the original estimate.
    public static final int GRID_LEFT = PANEL_LEFT + 8;
    public static final int GRID_TOP = PANEL_TOP - 1;
    public static final int SLOT_SIZE = 18;
    public static final int COLUMNS = 4;

    private BackpackPanelLayout() {
    }

    public static int slotX(int index) {
        return GRID_LEFT + (index % COLUMNS) * SLOT_SIZE;
    }

    public static int slotY(int index) {
        return GRID_TOP + (index / COLUMNS) * SLOT_SIZE;
    }
}
