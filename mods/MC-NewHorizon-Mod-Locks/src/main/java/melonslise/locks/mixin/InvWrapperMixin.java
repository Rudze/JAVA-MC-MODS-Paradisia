package melonslise.locks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import melonslise.locks.common.util.LocksUtil;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * Forge's old BlockEntity#getCapability() mixin hook (used to veto locked chests/furnaces from being
 * exposed to modded automation) has no NeoForge equivalent: block ItemHandler capabilities are resolved
 * through an additive provider chain (first non-null wins) that NeoForge's own vanilla-container providers
 * populate before any mod gets a chance to run, so a competing provider can never "deny" what vanilla
 * already returned. Instead, this denies the actual mutation at its choke point: NeoForge wraps every
 * vanilla Container (chests, furnaces, and other generic containers) passed through the capability system
 * in InvWrapper (or SidedInvWrapper, see {@link SidedInvWrapperMixin}) — blocking insert/extract there
 * covers all of them from one place.
 */
@Mixin(InvWrapper.class)
public class InvWrapperMixin
{
	@Shadow
	public Container getInv() { throw new AssertionError(); }

	@Inject(at = @At("HEAD"), method = "insertItem", cancellable = true)
	private void locks$insertItem(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir)
	{
		if (this.getInv() instanceof BlockEntity be && be.hasLevel() && LocksUtil.locked(be.getLevel(), be.getBlockPos()))
			cir.setReturnValue(stack);
	}

	@Inject(at = @At("HEAD"), method = "extractItem", cancellable = true)
	private void locks$extractItem(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir)
	{
		if (this.getInv() instanceof BlockEntity be && be.hasLevel() && LocksUtil.locked(be.getLevel(), be.getBlockPos()))
			cir.setReturnValue(ItemStack.EMPTY);
	}
}
