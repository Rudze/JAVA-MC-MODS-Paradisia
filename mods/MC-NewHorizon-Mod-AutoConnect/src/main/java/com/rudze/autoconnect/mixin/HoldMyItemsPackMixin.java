package com.rudze.autoconnect.mixin;

import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hold My Items registers its "HMI 3D Buckets" resource pack as required, so it is always on and
 * can't be turned off from the pack screen. Skipping its registration keeps the mod's held-item
 * rendering without the pack.
 */
@Pseudo
@Mixin(targets = "de.bene2212.holdmyitemsnf.Holdmyitemsnf")
public abstract class HoldMyItemsPackMixin {

    @Inject(method = "addPackFinders", at = @At("HEAD"), cancellable = true)
    private void nh_autoconnect$skip3dBuckets(AddPackFindersEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
