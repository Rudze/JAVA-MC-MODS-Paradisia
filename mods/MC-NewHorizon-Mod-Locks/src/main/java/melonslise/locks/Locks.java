package melonslise.locks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import melonslise.locks.common.config.LocksClientConfig;
import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.config.LocksServerConfig;
import melonslise.locks.common.init.LocksAttachments;
import melonslise.locks.common.init.LocksBlocks;
import melonslise.locks.common.init.LocksConditions;
import melonslise.locks.common.init.LocksCreativeTabs;
import melonslise.locks.common.init.LocksFeatures;
import melonslise.locks.common.init.LocksItems;
import melonslise.locks.common.init.LocksMenuTypes;
import melonslise.locks.common.init.LocksNetwork;
import melonslise.locks.common.init.LocksPlacements;
import melonslise.locks.common.init.LocksRecipeSerializers;
import melonslise.locks.common.init.LocksSoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Locks.ID)
public final class Locks
{
	public static final String ID = "locks";

	public static final Logger LOGGER = LogManager.getLogger();

	public Locks(IEventBus bus, Dist dist, ModContainer container)
	{
		container.registerConfig(ModConfig.Type.SERVER, LocksServerConfig.SPEC);
		container.registerConfig(ModConfig.Type.COMMON, LocksConfig.SPEC);
		container.registerConfig(ModConfig.Type.CLIENT, LocksClientConfig.SPEC);

		LocksAttachments.register(bus);
		LocksConditions.register(bus);
		LocksBlocks.register(bus);
		LocksItems.register(bus);
		LocksSoundEvents.register(bus);
		LocksPlacements.register(bus);
		LocksFeatures.register(bus);
		LocksMenuTypes.register(bus);
		LocksRecipeSerializers.register(bus);
		LocksCreativeTabs.register(bus);

		bus.addListener(LocksNetwork::register);
	}
}
