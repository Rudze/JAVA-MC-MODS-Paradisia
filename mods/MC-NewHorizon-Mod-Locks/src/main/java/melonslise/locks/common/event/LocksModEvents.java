package melonslise.locks.common.event;

import melonslise.locks.Locks;
import melonslise.locks.common.capability.KeyRingInventory;
import melonslise.locks.common.compat.CarryOnCompat;
import melonslise.locks.common.compat.CuriosHelper;
import melonslise.locks.common.compat.RespawningStructuresCompat;
import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.config.LocksServerConfig;
import melonslise.locks.common.init.LockTypeRegistry;
import melonslise.locks.common.init.LocksItems;
import melonslise.locks.common.item.KeyRingItem;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(modid = Locks.ID)
public final class LocksModEvents
{
	private LocksModEvents() {}

	@SubscribeEvent
	public static void onSetup(FMLCommonSetupEvent e)
	{
		e.enqueueWork(CuriosHelper::init);
		e.enqueueWork(RespawningStructuresCompat::init);
		e.enqueueWork(CarryOnCompat::init);
	}

	@SubscribeEvent
	public static void onRegisterCaps(RegisterCapabilitiesEvent event)
	{
		event.registerItem(Capabilities.ItemHandler.ITEM,
			(stack, ctx) -> new KeyRingInventory(stack, ((KeyRingItem) stack.getItem()).rows, 9),
			LocksItems.KEY_RING.get());
	}

	@SubscribeEvent
	public static void onConfigLoad(ModConfigEvent e)
	{
		// Unloading fires (among other times) when a world/server shuts down, after the spec's backing
		// file has already been released - reading .get() at that point throws IllegalStateException and
		// takes the whole server down with it. Only Loading/Reloading carry a live, readable config.
		if(e instanceof ModConfigEvent.Unloading)
			return;
		if(e.getConfig().getSpec() == LocksConfig.SPEC)
		{
			LocksConfig.init();
			if (LockTypeRegistry.isLoaded())
				LockTypeRegistry.resetStats();
		}
		if(e.getConfig().getSpec() == LocksServerConfig.SPEC)
			LocksServerConfig.init();
	}
}
