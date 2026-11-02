package tallestred.numismaticoverhaul.client.gui.purse;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.layers.Layer;
import io.wispforest.owo.ui.parsing.UIModelLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.cap.CurrencyHolder;
import tallestred.numismaticoverhaul.client.gui.CurrencyTooltipComponent;
import tallestred.numismaticoverhaul.currency.Currency;
import tallestred.numismaticoverhaul.currency.CurrencyResolver;
import tallestred.numismaticoverhaul.item.CurrencyTooltipData;
import tallestred.numismaticoverhaul.network.RequestPurseActionC2SPacket;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public class PurseLayerElement<S extends Screen> implements Consumer<Layer<S, StackLayout>.Instance> {
    private final AlignmentFunction<S> alignmentFunction;

    public PurseLayerElement(AlignmentFunction<S> alignmentFunction) {
        this.alignmentFunction = alignmentFunction;
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public void accept(Layer<S, StackLayout>.Instance instance) {
        var popup = UIModelLoader.get(NumismaticOverhaul.id("purse")).expandTemplate(StackLayout.class, "popup", Map.of());
        this.alignmentFunction.align(instance, popup);

        var total = new MutableObject<LongSupplier>();
        var goldCount = configureSelector(
                popup.childById(LabelComponent.class, "gold-count"),
                popup.childById(ButtonComponent.class, "gold-increment"),
                popup.childById(ButtonComponent.class, "gold-decrement"),
                Currency.GOLD, total
        );

        var silverCount = configureSelector(
                popup.childById(LabelComponent.class, "silver-count"),
                popup.childById(ButtonComponent.class, "silver-increment"),
                popup.childById(ButtonComponent.class, "silver-decrement"),
                Currency.SILVER, total
        );

        var bronzeCount = configureSelector(
                popup.childById(LabelComponent.class, "bronze-count"),
                popup.childById(ButtonComponent.class, "bronze-increment"),
                popup.childById(ButtonComponent.class, "bronze-decrement"),
                Currency.BRONZE, total
        );

        total.setValue(() -> value(bronzeCount, silverCount, goldCount));

        popup.childById(ButtonComponent.class, "extract-button").onPress($ -> {
            long value = value(bronzeCount, silverCount, goldCount);

            if (Screen.hasShiftDown() && Screen.hasControlDown()) {
                NumismaticOverhaul.MY_CHANNEL.clientHandle().send(RequestPurseActionC2SPacket.extractAll());
            } else if (value > 0) {
                NumismaticOverhaul.MY_CHANNEL.clientHandle().send(RequestPurseActionC2SPacket.extract(value));
                CurrencyHolder.silentModify(Minecraft.getInstance().player, -value);

                adjust(total.getValue().getAsLong(), bronzeCount, Currency.BRONZE, 0, popup.childById(LabelComponent.class, "bronze-count"));
                adjust(total.getValue().getAsLong(), silverCount, Currency.SILVER, 0, popup.childById(LabelComponent.class, "silver-count"));
                adjust(total.getValue().getAsLong(), goldCount, Currency.GOLD, 0, popup.childById(LabelComponent.class, "gold-count"));
            }
        });

        var button = UIModelLoader.get(NumismaticOverhaul.id("purse")).expandTemplate(ButtonComponent.class, "button", Map.of());
        instance.adapter.rootComponent.child(button);
        this.alignmentFunction.align(instance, button);

        button.onPress(buttonComponent -> {
            if (Screen.hasShiftDown()) {
                NumismaticOverhaul.MY_CHANNEL.clientHandle().send(RequestPurseActionC2SPacket.storeAll());
            } else if (popup.hasParent()) {
                popup.remove();
            } else {
                instance.adapter.rootComponent.child(popup);
            }
        });

        // Refreshes the tooltip every frame this screen is open, so it always reflects the player's
        // current currency total (which can change from outside this popup - picking up coins,
        // another mod's UI, etc). Was originally Fabric's ScreenEvents.afterRender(screen).register(...)
        // (a per-screen-scoped callback, garbage collected with the screen) - that class doesn't
        // exist on NeoForge at all, crashing with NoClassDefFoundError the moment any screen opened.
        // NeoForge's event bus has no per-screen-instance scoping built in (addListener is global),
        // so this pair self-unregisters on ScreenEvent.Closing instead of leaking a listener per
        // screen ever opened.
        Consumer<ScreenEvent.Render.Post> renderListener = event -> {
            if (event.getScreen() != instance.screen) return;
            button.tooltip(List.of(ClientTooltipComponent.create(net.minecraft.network.chat.Component.translatable("gui.numismaticoverhaul.purse_title").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(Currency.GOLD.getNameColor()))).getVisualOrderText()), new CurrencyTooltipComponent(new CurrencyTooltipData(CurrencyHolder.getValue(Minecraft.getInstance().player), -1))));
        };
        Consumer<ScreenEvent.Closing> closeListener = new Consumer<>() {
            @Override
            public void accept(ScreenEvent.Closing event) {
                if (event.getScreen() != instance.screen) return;
                NeoForge.EVENT_BUS.unregister(renderListener);
                NeoForge.EVENT_BUS.unregister(this);
            }
        };
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, renderListener);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Closing.class, closeListener);
    }

    private static MutableInt configureSelector(LabelComponent display, ButtonComponent incrementButton, ButtonComponent decrementButton, Currency currency, MutableObject<LongSupplier> totalSupplier) {
        var value = new MutableInt();
        incrementButton.onPress($ -> adjust(totalSupplier.getValue().getAsLong(), value, currency, Screen.hasShiftDown() ? 10 : 1, display));
        decrementButton.onPress($ -> adjust(totalSupplier.getValue().getAsLong(), value, currency, Screen.hasShiftDown() ? -10 : -1, display));

        return value;
    }

    private static void adjust(long selectedTotal, MutableInt value, Currency currency, int adjustBy, LabelComponent display) {
        //noinspection DataFlowIssue
        long total = CurrencyHolder.getValue(Minecraft.getInstance().player);
        long stepSize = currency.getRawValue(1);
        long by = Math.min(adjustBy, (total - selectedTotal) / stepSize);

        value.setValue(Mth.clamp(value.intValue() + by, 0, Math.min(total / stepSize, 99)));
        display.text(net.minecraft.network.chat.Component.literal(String.valueOf(value.getValue())));
    }

    private static long value(MutableInt bronze, MutableInt silver, MutableInt gold) {
        return CurrencyResolver.combineValues(new long[]{bronze.intValue(), silver.intValue(), gold.intValue()});
    }

    @FunctionalInterface
    public interface AlignmentFunction<S extends Screen> {
        void align(Layer<S, StackLayout>.Instance instance, Component component);
    }
}