package melonslise.locks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import melonslise.locks.common.util.LocksUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin
{
	// 1.21.1 rewrote getContainerAt(Level,double,double,double) into a private
	// getContainerAt(Level,BlockPos,BlockState,double,double,double) - both getAttachedContainer (push target)
	// and getSourceContainer (pull source) now funnel through this one, so it's still the single choke point.
	@Inject(at = @At("HEAD"), method = "getContainerAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;DDD)Lnet/minecraft/world/Container;", cancellable = true)
	private static void getContainerAt(Level world, BlockPos pos, BlockState state, double x, double y, double z, CallbackInfoReturnable<Container> cir)
	{
		if(LocksUtil.locked(world, pos))
			cir.setReturnValue(null);
	}
}
