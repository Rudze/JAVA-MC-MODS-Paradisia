package com.rudze.backpack.client;

import com.rudze.backpack.item.BackpackItems;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Tints the backpack's base layer (model tintindex 0) with whatever color it's been dyed, mirroring
 * vanilla leather armor. Reusing vanilla's own {@code minecraft:dyeable} item tag (see
 * data/minecraft/tags/item/dyeable.json) is what actually makes the item craftable-dyeable at all -
 * vanilla's built-in ArmorDyeRecipe matches any item in that tag plus one or more dyes, no custom
 * recipe needed. This class only handles reading the result back out at render time.
 */
public final class BackpackColorHandler {

    private BackpackColorHandler() {
    }

    public static void register(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> tintIndex == 0
                        ? DyedItemColor.getOrDefault(stack, DyedItemColor.LEATHER_COLOR)
                        : 0xFFFFFF,
                BackpackItems.BACKPACK.get());
    }
}
