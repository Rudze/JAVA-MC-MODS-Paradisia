package tallestred.numismaticoverhaul.init;

import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.block.PiggyBankScreenHandler;
import tallestred.numismaticoverhaul.block.ShopScreenHandler;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class MenuInit {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, NumismaticOverhaul.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<ShopScreenHandler>> SHOP = MENU_TYPES.register("shop", () -> new MenuType<>(ShopScreenHandler::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<PiggyBankScreenHandler>> PIGGY_BANK = MENU_TYPES.register("piggy_bank", () -> new MenuType<>(PiggyBankScreenHandler::new, FeatureFlags.DEFAULT_FLAGS));
}
