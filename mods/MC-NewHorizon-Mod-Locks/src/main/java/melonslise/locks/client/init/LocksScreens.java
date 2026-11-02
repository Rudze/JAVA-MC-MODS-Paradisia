package melonslise.locks.client.init;

import melonslise.locks.client.gui.KeyRingScreen;
import melonslise.locks.client.gui.LockPickingScreen;
import melonslise.locks.common.init.LocksMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@OnlyIn(Dist.CLIENT)
public final class LocksScreens
{
	private LocksScreens() {}

	public static void register(RegisterMenuScreensEvent event)
	{
		event.register(LocksMenuTypes.LOCK_PICKING.get(), LockPickingScreen::new);
		event.register(LocksMenuTypes.KEY_RING.get(), KeyRingScreen::new);
	}
}
