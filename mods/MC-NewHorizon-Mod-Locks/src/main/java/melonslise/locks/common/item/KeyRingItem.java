package melonslise.locks.common.item;

import melonslise.locks.common.container.KeyRingContainer;
import melonslise.locks.common.util.LocksNbtUtil;
import melonslise.locks.common.util.LocksUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

public class KeyRingItem extends Item
{
	public final int rows;

	public KeyRingItem(int rows, Properties props)
	{
		super(props.stacksTo(1));
		this.rows = rows;
	}

	public static boolean containsId(ItemStack stack, int id)
	{
		ListTag list = LocksNbtUtil.getTag(stack).getList("Items", Tag.TAG_COMPOUND);
		for(int a = 0; a < list.size(); ++a)
		{
			ItemStack keyStack = ItemStack.parseOptional(LocksUtil.currentRegistries(), list.getCompound(a));
			if(!keyStack.isEmpty() && LocksNbtUtil.hasTag(keyStack) && LocksNbtUtil.getTag(keyStack).contains(LockingItem.KEY_ID)
				&& LockingItem.getOrSetId(keyStack) == id)
				return true;
		}
		return false;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		if(!player.level().isClientSide)
			player.openMenu(new KeyRingContainer.Provider(stack), new KeyRingContainer.Writer(hand));
		return new InteractionResultHolder<>(InteractionResult.PASS, stack);
	}

}
