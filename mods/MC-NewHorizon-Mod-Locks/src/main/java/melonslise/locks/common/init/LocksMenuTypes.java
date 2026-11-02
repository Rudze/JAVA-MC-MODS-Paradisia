package melonslise.locks.common.init;

import melonslise.locks.Locks;
import melonslise.locks.common.container.KeyRingContainer;
import melonslise.locks.common.container.LockPickingContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LocksMenuTypes
{
	public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, Locks.ID);

	public static final DeferredHolder<MenuType<?>, MenuType<LockPickingContainer>>
		LOCK_PICKING = MENU_TYPES.register("lock_picking", () -> IMenuTypeExtension.create(LockPickingContainer.FACTORY));

	public static final DeferredHolder<MenuType<?>, MenuType<KeyRingContainer>>
		KEY_RING = MENU_TYPES.register("key_ring", () -> IMenuTypeExtension.create(KeyRingContainer.FACTORY));

	private LocksMenuTypes() {}

	public static void register(IEventBus bus)
	{
		MENU_TYPES.register(bus);
	}
}
