package melonslise.locks.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import melonslise.locks.common.util.LocksUtil;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

/** See {@link InvWrapperMixin} — same veto, for the WorldlyContainer (sided) wrapper family. */
@Mixin(SidedInvWrapper.class)
public class SidedInvWrapperMixin
{
	@Shadow
	@Final
	protected WorldlyContainer inv;

	@Inject(at = @At("HEAD"), method = "insertItem", cancellable = true)
	private void locks$insertItem(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir)
	{
		if (this.inv instanceof BlockEntity be && be.hasLevel() && LocksUtil.locked(be.getLevel(), be.getBlockPos()))
			cir.setReturnValue(stack);
	}

	@Inject(at = @At("HEAD"), method = "extractItem", cancellable = true)
	private void locks$extractItem(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir)
	{
		if (this.inv instanceof BlockEntity be && be.hasLevel() && LocksUtil.locked(be.getLevel(), be.getBlockPos()))
			cir.setReturnValue(ItemStack.EMPTY);
	}
}
