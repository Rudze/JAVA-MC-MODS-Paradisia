package tallestred.numismaticoverhaul.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.item.MoneyBagItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CreativeTabInit {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NumismaticOverhaul.MODID);
    public static final List<Supplier<? extends ItemLike>> TAB_ITEMS = new ArrayList<>();
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NUMISMATIC_GROUP = TABS.register("numismatic_group",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.numismaticoverhaul"))
                    .icon(() -> MoneyBagItem.fromValues(new long[]{0,1,0}))
                    .displayItems((displayParams, output) ->
                            TAB_ITEMS.forEach(itemLike -> output.accept(itemLike.get())))
                    .withSearchBar()
                    .build()
    );

    public static <T extends ItemLike> DeferredHolder<T,T> addToTab(DeferredHolder<T,T> itemLike) {
        TAB_ITEMS.add(itemLike);
        return itemLike;
    }
}
