package melonslise.locks.common.item;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import melonslise.locks.Locks;
import melonslise.locks.common.config.LocksServerConfig;
import melonslise.locks.common.util.LocksNbtUtil;
import melonslise.locks.common.util.LocksUtil;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class LockingItem extends Item
{
	public LockingItem(Properties props)
	{
		super(props.stacksTo(1));
	}

	public static final String KEY_ID = "Id";

	public static ItemStack copyId(ItemStack from, ItemStack to)
	{
		CompoundTag nbt = LocksNbtUtil.getTag(to);
		nbt.putInt(KEY_ID, getOrSetId(from));
		LocksNbtUtil.setTag(to, nbt);
		return to;
	}

	public static int getOrSetId(ItemStack stack)
	{
		CompoundTag nbt = LocksNbtUtil.getTag(stack);
		if(!nbt.contains(KEY_ID))
		{
			nbt.putInt(KEY_ID, ThreadLocalRandom.current().nextInt());
			LocksNbtUtil.setTag(stack, nbt);
		}
		return nbt.getInt(KEY_ID);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected)
	{
		if(!world.isClientSide)
			getOrSetId(stack);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> lines, TooltipFlag flag)
	{
		if(!LocksServerConfig.HIDE_LOCK_ID.get() && LocksNbtUtil.hasTag(stack) && LocksNbtUtil.getTag(stack).contains(KEY_ID))
			lines.add(Component.translatable(Locks.ID + ".tooltip.id", LocksUtil.TOOLTIP_NUMBER_FORMAT.format(getOrSetId(stack))).withStyle(ChatFormatting.DARK_GREEN));
	}
}
