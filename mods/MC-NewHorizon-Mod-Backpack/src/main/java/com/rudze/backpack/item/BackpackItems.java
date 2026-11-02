package com.rudze.backpack.item;

import com.rudze.backpack.BackpackMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BackpackItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BackpackMod.MODID);

    public static final DeferredHolder<Item, BackpackItem> BACKPACK = ITEMS.register(
            "backpack", () -> new BackpackItem(new Item.Properties().stacksTo(1)));

    private BackpackItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(BackpackItems::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(BACKPACK.get());
        }
    }
}
