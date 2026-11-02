package melonslise.locks.client.init;

import melonslise.locks.Locks;
import melonslise.locks.common.init.LocksItems;
import melonslise.locks.common.item.LockItem;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

@OnlyIn(Dist.CLIENT)
public final class LocksItemModelsProperties
{
	private LocksItemModelsProperties() {}

	public static void register()
	{
		ItemProperties.register(LocksItems.KEY_RING.get(), ResourceLocation.fromNamespaceAndPath(Locks.ID, "keys"), (stack, level, entity, seed) ->
		{
			IItemHandler inv = stack.getCapability(Capabilities.ItemHandler.ITEM);
			if (inv == null)
				return 0f;
			int keys = 0;
			for(int a = 0; a < inv.getSlots(); ++a)
				if(!inv.getStackInSlot(a).isEmpty())
					++keys;
			return (float) keys / inv.getSlots();
		});

		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Locks.ID, "open");
		ClampedItemPropertyFunction getter = (stack, level, entity, seed) -> LockItem.isOpen(stack) ? 1f : 0f;

		// Register the "open" property for all data-driven locks
		for (var entry : LocksItems.getLockItems().values())
			ItemProperties.register(entry.get(), id, getter);
	}
}
