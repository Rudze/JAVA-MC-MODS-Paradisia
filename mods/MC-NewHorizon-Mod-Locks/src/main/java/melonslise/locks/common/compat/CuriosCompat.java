package melonslise.locks.common.compat;

import java.util.List;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import melonslise.locks.common.init.LocksItems;
import melonslise.locks.common.item.LockingItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

// Accessories (not Curios) is the wearable-slot mod this modpack standardizes on.
public final class CuriosCompat
{
	private CuriosCompat() {}

	public static ItemStack findMatchingKeyRing(Player player, int lockId)
	{
		for (SlotEntryReference entry : equippedKeyRings(player))
		{
			ItemStack ring = entry.stack();
			IItemHandler handler = ring.getCapability(Capabilities.ItemHandler.ITEM);
			if (handler == null)
				continue;
			for (int i = 0; i < handler.getSlots(); i++)
			{
				ItemStack key = handler.getStackInSlot(i);
				if (!key.isEmpty() && LockingItem.getOrSetId(key) == lockId)
					return ring;
			}
		}
		return ItemStack.EMPTY;
	}

	public static ItemStack findAnyKeyRing(Player player)
	{
		List<SlotEntryReference> found = equippedKeyRings(player);
		return found.isEmpty() ? ItemStack.EMPTY : found.get(0).stack();
	}

	private static List<SlotEntryReference> equippedKeyRings(Player player)
	{
		return AccessoriesCapability.getOptionally(player)
			.map(cap -> cap.getEquipped(LocksItems.KEY_RING.get()))
			.orElse(List.of());
	}
}
