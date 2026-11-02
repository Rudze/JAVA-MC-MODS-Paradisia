package tallestred.numismaticoverhaul.mixin;

import io.wispforest.owo.ops.ItemOps;
import tallestred.numismaticoverhaul.cap.CurrencyHolder;
import tallestred.numismaticoverhaul.config.NOConfig;
import tallestred.numismaticoverhaul.currency.CurrencyConverter;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "destroyVanishingCursedItems", at = @At("TAIL"))
    public void onServerDeath(CallbackInfo ci) {
        var player = (Player) (Object) this;

        final var world = player.level();
        if (world.isClientSide) return;


        var dropPercentage = NOConfig.INSTANCE.moneyDropChance.get().floatValue() * .01f;
        int dropped = (int) (CurrencyHolder.getValue(player) * dropPercentage);

        var stacksDropped = CurrencyConverter.getAsValidStacks(dropped);
        for (var drop : stacksDropped) {
            for (int i = 0; i < drop.getCount(); i++) {
                player.drop(ItemOps.singleCopy(drop), true, false);
            }
        }

        CurrencyHolder.modify(player, -dropped);
    }

}
