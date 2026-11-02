package fr.rudze.furniture.block;

import fr.rudze.furniture.FurnitureMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FurnitureMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FURNITURE = TABS.register(
            "furniture",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nh_furniture.furniture"))
                    .icon(() -> ModBlocks.furnitureItems().isEmpty()
                            ? ItemStack.EMPTY
                            : new ItemStack(ModBlocks.furnitureItems().iterator().next().get()))
                    .displayItems((params, output) -> ModBlocks.furnitureItems().forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
