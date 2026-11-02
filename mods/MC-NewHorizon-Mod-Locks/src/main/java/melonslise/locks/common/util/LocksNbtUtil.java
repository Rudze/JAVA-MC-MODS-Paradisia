package melonslise.locks.common.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Item NBT storage was rewritten around DataComponents in 1.20.5+, removing the old
 * ItemStack#getOrCreateTag/getTag/hasTag trio. This mirrors their exact contract on top of the
 * DataComponents#CUSTOM_DATA component so every former {@code stack.getOrCreateTag().putX(...)} call site
 * only needs its read/write calls swapped for {@link #getTag}/{@link #setTag} (mutations are never visible
 * until explicitly written back with {@link #setTag}, unlike the old live-reference semantics).
 */
public final class LocksNbtUtil
{
	private LocksNbtUtil() {}

	public static CompoundTag getTag(ItemStack stack)
	{
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	public static boolean hasTag(ItemStack stack)
	{
		return stack.has(DataComponents.CUSTOM_DATA);
	}

	public static void setTag(ItemStack stack, CompoundTag tag)
	{
		CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
	}
}
