package melonslise.locks.common.capability;

import javax.annotation.Nonnull;

import melonslise.locks.common.init.LocksTagHelper;
import melonslise.locks.common.util.LocksNbtUtil;
import melonslise.locks.common.util.LocksUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

// Thanks to Gigaherz
public class KeyRingInventory implements IItemHandlerModifiable
{
	public final int size;
	public final ItemStack stack;
	private int[] slotToListIndex;

	public KeyRingInventory(ItemStack stack, int rows, int col)
	{
		this.size = rows * col;
		this.stack = stack;
	}

	@Override
	public int getSlots()
	{
		return this.size;
	}

	private void ensureIndex()
	{
		if(this.slotToListIndex != null)
			return;
		int[] index = new int[this.size];
		for(int i = 0; i < this.size; i++)
			index[i] = -1;
		ListTag list = LocksNbtUtil.getTag(this.stack).getList("Items", Tag.TAG_COMPOUND);
		for(int a = 0; a < list.size(); a++)
		{
			int slot = list.getCompound(a).getInt("Slot");
			if(slot >= 0 && slot < this.size)
				index[slot] = a;
		}
		this.slotToListIndex = index;
	}

	private void invalidateIndex()
	{
		this.slotToListIndex = null;
	}

	@Override
	public ItemStack getStackInSlot(int slot)
	{
		this.validateSlotIndex(slot);
		this.ensureIndex();
		int idx = this.slotToListIndex[slot];
		if(idx == -1)
			return ItemStack.EMPTY;
		ListTag list = LocksNbtUtil.getTag(this.stack).getList("Items", Tag.TAG_COMPOUND);
		return ItemStack.parseOptional(LocksUtil.currentRegistries(), list.getCompound(idx));
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack)
	{
		this.validateSlotIndex(slot);
		this.invalidateIndex();
		CompoundTag nbt = null;
		if(!stack.isEmpty())
		{
			nbt = new CompoundTag();
			nbt.putInt("Slot", slot);
			stack.save(LocksUtil.currentRegistries(), nbt);
		}
		CompoundTag root = LocksNbtUtil.getTag(this.stack);
		ListTag list = root.getList("Items", Tag.TAG_COMPOUND);
		boolean replaced = false;
		for(int a = 0; a < list.size(); a++)
		{
			CompoundTag existing = list.getCompound(a);
			if(existing.getInt("Slot") != slot)
				continue;
			if(!stack.isEmpty())
				list.set(a, nbt);
			else
				list.remove(a);
			replaced = true;
			break;
		}
		if(!replaced && !stack.isEmpty())
			list.add(nbt);
		root.put("Items", list);
		LocksNbtUtil.setTag(this.stack, root);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
	{
		if (stack.isEmpty())
			return ItemStack.EMPTY;
		this.validateSlotIndex(slot);
		ItemStack existing = getStackInSlot(slot);
		int limit = stack.getMaxStackSize();
		if (!existing.isEmpty())
		{
			if (!ItemStack.isSameItemSameComponents(stack, existing))
				return stack;
			limit -= existing.getCount();
		}
		if (limit <= 0)
			return stack;
		boolean reachedLimit = stack.getCount() > limit;
		if (!simulate)
		{
			if (existing.getCount() <= 0)
				existing = reachedLimit ? stack.copyWithCount(limit) : stack;
			else
				existing.grow(reachedLimit ? limit : stack.getCount());
			this.setStackInSlot(slot, existing);
		}
		return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate)
	{
		if (amount == 0)
			return ItemStack.EMPTY;
		this.validateSlotIndex(slot);
		ItemStack existing = this.getStackInSlot(slot);
		if (existing.isEmpty())
			return ItemStack.EMPTY;
		int toExtract = Math.min(amount, existing.getCount());
		if (existing.getCount() <= toExtract)
		{
			if (!simulate)
					this.setStackInSlot(slot, ItemStack.EMPTY);
			return existing;
		}
		else
		{
			if (!simulate)
				this.setStackInSlot(slot, existing.copyWithCount(existing.getCount() - toExtract));
			return existing.copyWithCount(toExtract);
		}
	}

	@Override
	public int getSlotLimit(int slot)
	{
		return 64;
	}

	private void validateSlotIndex(int slot)
	{
		if (slot < 0 || slot >= this.getSlots())
			throw new RuntimeException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack)
	{
		return LocksTagHelper.isKey(stack);
	}
}
