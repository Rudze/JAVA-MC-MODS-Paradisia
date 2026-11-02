package tallestred.numismaticoverhaul.event;

import io.wispforest.owo.ui.parsing.UIModelLoader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.client.gui.CurrencyTooltipComponent;
import tallestred.numismaticoverhaul.item.CurrencyTooltipData;

@EventBusSubscriber(modid = NumismaticOverhaul.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onTooltip(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CurrencyTooltipData.class, CurrencyTooltipComponent::new);
    }
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new UIModelLoader());
    }
}
