package tallestred.numismaticoverhaul.mixin;

import net.neoforged.neoforge.common.VillagerTradingManager;
import tallestred.numismaticoverhaul.config.NOConfig;
import tallestred.numismaticoverhaul.villagers.data.NumismaticVillagerTradesRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerTradingManager.class)
public class VillagerTradingManagerMixin {
    @Inject(method = "loadTrades", at = @At("TAIL"), remap = false)
    private static void loadTrades(CallbackInfo ci) {
        if (!NOConfig.INSTANCE.enableVillagerTrading.get()) return;
        NumismaticVillagerTradesRegistry.wrapModVillagers();
    }
}
