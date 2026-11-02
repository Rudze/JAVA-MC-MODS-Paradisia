package melonslise.locks.common.init;

import com.mojang.serialization.MapCodec;

import melonslise.locks.Locks;
import melonslise.locks.common.steel.NativeSteelCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class LocksConditions
{
	public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Locks.ID);

	public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<NativeSteelCondition>> STEEL = CONDITION_CODECS.register("steel", () -> NativeSteelCondition.CODEC);

	private LocksConditions() {}

	public static void register(IEventBus bus)
	{
		CONDITION_CODECS.register(bus);
	}
}
