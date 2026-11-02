package melonslise.locks.common.init;

import melonslise.locks.Locks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Native steel ore blocks. These are ALWAYS registered under stable IDs regardless of the configured
 * {@link melonslise.locks.common.steel.SteelMaterialMode} or any installed steel mod — only their generation and
 * production recipes are gated by {@link melonslise.locks.common.steel.NativeSteelPolicy}. Registered from
 * {@link melonslise.locks.Locks} before {@code LocksItems} so the block items resolve.
 *
 * <p>Properties mirror vanilla iron / deepslate iron ore. The blocks drop themselves (standard Silk-Touch-safe
 * block loot) and are smelted/blasted into {@code locks:steel_ingot}.
 */
public final class LocksBlocks
{
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Locks.ID);
	public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(Registries.ITEM, Locks.ID);

	public static final DeferredHolder<Block, Block> STEEL_ORE = BLOCKS.register("steel_ore",
		() -> new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.requiresCorrectToolForDrops()
			.strength(3.0F, 3.0F)
			.sound(SoundType.STONE)));

	public static final DeferredHolder<Block, Block> DEEPSLATE_STEEL_ORE = BLOCKS.register("deepslate_steel_ore",
		() -> new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.DEEPSLATE)
			.requiresCorrectToolForDrops()
			.strength(4.5F, 3.0F)
			.sound(SoundType.DEEPSLATE)));

	public static final DeferredHolder<Item, ? extends Item> STEEL_ORE_ITEM = BLOCK_ITEMS.register("steel_ore",
		() -> new BlockItem(STEEL_ORE.get(), new Item.Properties()));

	public static final DeferredHolder<Item, ? extends Item> DEEPSLATE_STEEL_ORE_ITEM = BLOCK_ITEMS.register("deepslate_steel_ore",
		() -> new BlockItem(DEEPSLATE_STEEL_ORE.get(), new Item.Properties()));

	private LocksBlocks() {}

	public static void register(IEventBus bus)
	{
		BLOCKS.register(bus);
		BLOCK_ITEMS.register(bus);
	}
}
