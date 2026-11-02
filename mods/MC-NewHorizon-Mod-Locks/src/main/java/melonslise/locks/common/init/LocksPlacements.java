package melonslise.locks.common.init;

import melonslise.locks.Locks;
import melonslise.locks.common.worldgen.ChestPlacement;
import melonslise.locks.common.worldgen.SteelOrePlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LocksPlacements
{
	public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Locks.ID);

	public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<ChestPlacement>>
		CHEST = PLACEMENT_MODIFIERS.register("chest", () -> () -> ChestPlacement.CODEC);

	public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<SteelOrePlacement>>
		STEEL_ORE = PLACEMENT_MODIFIERS.register("steel_ore", () -> () -> SteelOrePlacement.CODEC);

	private LocksPlacements() {}

	public static void register(IEventBus bus)
	{
		PLACEMENT_MODIFIERS.register(bus);
	}
}
