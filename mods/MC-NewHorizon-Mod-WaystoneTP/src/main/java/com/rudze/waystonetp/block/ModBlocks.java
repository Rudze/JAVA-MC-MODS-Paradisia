package com.rudze.waystonetp.block;

import com.rudze.waystonetp.WaystoneTPMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WaystoneTPMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WaystoneTPMod.MODID);

    public static final DeferredBlock<WaystoneBlock> WAYSTONE_A = BLOCKS.registerBlock(
            "waystone_a", props -> new WaystoneBlock(WaystoneVariant.A, props), waystoneProperties());
    public static final DeferredBlock<WaystoneBlock> WAYSTONE_B = BLOCKS.registerBlock(
            "waystone_b", props -> new WaystoneBlock(WaystoneVariant.B, props), waystoneProperties());

    public static final DeferredItem<BlockItem> WAYSTONE_A_ITEM = ITEMS.registerSimpleBlockItem(WAYSTONE_A);
    public static final DeferredItem<BlockItem> WAYSTONE_B_ITEM = ITEMS.registerSimpleBlockItem(WAYSTONE_B);

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties waystoneProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE)
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .noOcclusion();
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModBlocks::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(WAYSTONE_A_ITEM.get());
            event.accept(WAYSTONE_B_ITEM.get());
        }
    }
}
