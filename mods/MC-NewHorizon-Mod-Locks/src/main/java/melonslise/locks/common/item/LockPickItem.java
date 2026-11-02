package melonslise.locks.common.item;

import java.util.List;

import melonslise.locks.Locks;
import melonslise.locks.common.config.LocksServerConfig;
import melonslise.locks.common.init.LocksEnchantments;
import melonslise.locks.common.init.LocksTagHelper;
import melonslise.locks.common.init.LockTypeRegistry;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.LocksNbtUtil;
import melonslise.locks.common.util.LocksUtil;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class LockPickItem extends Item
{
	public static final Component TOO_COMPLEX_MESSAGE = Component.translatable(Locks.ID + ".status.too_complex");
	public static final ResourceLocation NETHERITE_LOCK_PICK_ID = ResourceLocation.fromNamespaceAndPath(Locks.ID, "netherite_lock_pick");
	public static final int NETHERITE_DURABILITY = 128;

	public LockPickItem(Properties props)
	{
		super(props);
	}

	public static final String KEY_STRENGTH = "Strength";

	// WARNING: EXPECTS LOCKPICKITEM STACK
	public static float getOrSetStrength(ItemStack stack)
	{
		CompoundTag nbt = LocksNbtUtil.getTag(stack);
		if(!nbt.contains(KEY_STRENGTH))
		{
			nbt.putFloat(KEY_STRENGTH, LockTypeRegistry.getLockPickStats(stack.getItem()).strength());
			LocksNbtUtil.setTag(stack, nbt);
		}
		return nbt.getFloat(KEY_STRENGTH);
	}

	public static boolean canPick(RegistryAccess registries, ItemStack stack, int cmp)
	{
		float strength = getOrSetStrength(stack);
		if(LocksServerConfig.ENABLE_ATTUNEMENT.get())
			strength += LocksEnchantments.getLevel(registries, LocksEnchantments.ATTUNEMENT, stack)
				* (float) (double) LocksServerConfig.ATTUNEMENT_STRENGTH_PER_LEVEL.get();
		return strength > cmp * 0.25f;
	}

	public static boolean canPick(RegistryAccess registries, ItemStack stack, Lockable lkb)
	{
		return canPick(registries, stack, LocksServerConfig.ENABLE_COMPLEXITY.get()
			? LocksEnchantments.getLevel(registries, LocksEnchantments.COMPLEXITY, lkb.stack) : 0);
	}

	public static boolean usesDurability(ItemStack stack)
	{
		return stack.isDamageableItem() && isNetheriteLockPick(stack);
	}

	public static void damagePick(ItemStack stack, Player player, InteractionHand hand)
	{
		stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
	}

	public static boolean isNetheriteLockPick(ItemStack stack)
	{
		return isNetheriteLockPick(stack.getItem());
	}

	private static boolean isNetheriteLockPick(Item item)
	{
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
		return NETHERITE_LOCK_PICK_ID.equals(itemId);
	}

	@Override
	public boolean isEnchantable(ItemStack stack)
	{
		return stack.getCount() == 1 && LocksTagHelper.isLockPick(stack);
	}

	@Override
	public int getEnchantmentValue()
	{
		int value = LockTypeRegistry.getLockPickStats(this).enchantmentValue();
		return value > 0 ? value : super.getEnchantmentValue();
	}

	@Override
	public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair)
	{
		return isNetheriteLockPick(toRepair) && repair.is(Items.NETHERITE_INGOT) || super.isValidRepairItem(toRepair, repair);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> lines, TooltipFlag flag)
	{
		super.appendHoverText(stack, ctx, lines, flag);
		float displayStrength;
		if (LocksNbtUtil.hasTag(stack) && LocksNbtUtil.getTag(stack).contains(KEY_STRENGTH))
			displayStrength = LocksNbtUtil.getTag(stack).getFloat(KEY_STRENGTH);
		else
			displayStrength = LockTypeRegistry.getLockPickStats(this).strength();
		lines.add(Component.translatable(Locks.ID + ".tooltip.strength", LocksUtil.TOOLTIP_NUMBER_FORMAT.format(displayStrength)).withStyle(ChatFormatting.DARK_GREEN));
	}
}
