package tallestred.numismaticoverhaul.init;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.block.PiggyBankBlock;
import tallestred.numismaticoverhaul.block.PiggyBankBlockEntity;
import tallestred.numismaticoverhaul.block.ShopBlock;
import tallestred.numismaticoverhaul.block.ShopBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Function;
import java.util.function.Supplier;

import static tallestred.numismaticoverhaul.init.CreativeTabInit.addToTab;

public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, NumismaticOverhaul.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NumismaticOverhaul.MODID);

    public static final DeferredHolder<PiggyBankBlock, PiggyBankBlock> PIGGY_BANK = registerBlock("piggy_bank", PiggyBankBlock::new);
    // SHOP block
    public static final DeferredHolder<ShopBlock, ShopBlock> SHOP = registerBlock("shop", () -> new ShopBlock(false));
    //inexhastible shop block
    public static final DeferredHolder<ShopBlock, ShopBlock> INEXHAUSTIBLE_SHOP = registerBlock("inexhaustible_shop", () -> new ShopBlock(true));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PiggyBankBlockEntity>> PIGGY_BANK_BE = BLOCK_ENTITIES.register("piggy_bank", () -> BlockEntityType.Builder.of(PiggyBankBlockEntity::new, PIGGY_BANK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShopBlockEntity>> SHOP_BE = BLOCK_ENTITIES.register("shop", () -> BlockEntityType.Builder.of(ShopBlockEntity::new, SHOP.get(), INEXHAUSTIBLE_SHOP.get()).build(null));

    protected static <T extends Block> DeferredHolder<T, T> registerBlock(String name, Supplier<T> block) {
        return addToTab(registerBlock(name, block, b -> () -> new BlockItem(b.get(), new Item.Properties())));
    }

    protected static <T extends Block> DeferredHolder<T, T> registerBlock(String name, Supplier<T> block, Function<DeferredHolder<T, T>, Supplier<? extends BlockItem>> item) {
        var reg = BLOCKS.register(name, block);
        ItemInit.ITEMS.register(name, () -> item.apply((DeferredHolder<T, T>) reg).get());
        return (DeferredHolder<T, T>) reg;
    }
}


