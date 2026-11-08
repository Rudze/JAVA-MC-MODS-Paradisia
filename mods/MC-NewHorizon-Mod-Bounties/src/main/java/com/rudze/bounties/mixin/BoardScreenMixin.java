package com.rudze.bounties.mixin;

import io.ejekta.bountiful.client.BoardScreen;
import io.ejekta.kambrik.gui.draw.KGuiDsl;
import io.ejekta.kambrik.gui.draw.KWidget;
import kotlin.jvm.functions.Function1;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shows only the right half of the bounty board screen: the bounty papers, the decrees and the
 * player's inventory, without the list of bounties on the left.
 *
 * <p>Slots sit at fixed offsets from {@code leftPos}, so rather than moving them the screen keeps
 * its full width and is shifted left until the visible half is centred; the left half just isn't
 * drawn. Its left border is copied from the texture's own so the half still looks framed.
 */
@Mixin(BoardScreen.class)
public abstract class BoardScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("bountiful", "board_bg");
    private static final int TEXTURE_WIDTH = 349;
    private static final int TEXTURE_HEIGHT = 166;
    /** Width of the texture's left border, drawn again in front of the right half. */
    private static final int BORDER = 4;
    /** Where the right half starts in the texture (and in screen coordinates from leftPos). */
    private static final int CUT = 174;
    private static final int SHOWN_LEFT = CUT - BORDER;
    private static final int SHOWN_WIDTH = BORDER + TEXTURE_WIDTH - CUT;
    /** x offset Bountiful draws the list's scrollbar at; the other offsets are on the right half. */
    private static final int SCROLLBAR_X = 166;

    private BoardScreenMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void nh_bounties$centreRightHalf(CallbackInfo ci) {
        leftPos = (width - SHOWN_WIDTH) / 2 - SHOWN_LEFT;
    }

    @Inject(method = "onDrawBackground", at = @At("HEAD"), cancellable = true)
    private void nh_bounties$drawRightHalf(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        graphics.blitSprite(TEXTURE, TEXTURE_WIDTH, TEXTURE_HEIGHT, 0, 0, leftPos + SHOWN_LEFT, topPos, BORDER, TEXTURE_HEIGHT);
        graphics.blitSprite(TEXTURE, TEXTURE_WIDTH, TEXTURE_HEIGHT, CUT, 0, leftPos + CUT, topPos, TEXTURE_WIDTH - CUT, TEXTURE_HEIGHT);
        ci.cancel();
    }

    /** The title and the "no bounties" hint, both over the list. */
    @Redirect(method = "drawGui$lambda$25", at = @At(value = "INVOKE",
            target = "Lio/ejekta/kambrik/gui/draw/KGuiDsl;textCentered(IILkotlin/jvm/functions/Function1;)V"))
    private static void nh_bounties$hideListText(KGuiDsl gui, int x, int y, Function1<?, ?> text) {
    }

    @Redirect(method = "drawGui$lambda$25", at = @At(value = "INVOKE",
            target = "Lio/ejekta/kambrik/gui/draw/KGuiDsl;widget(Lio/ejekta/kambrik/gui/draw/KWidget;II)V"))
    private static void nh_bounties$hideList(KGuiDsl gui, KWidget widget, int x, int y) {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(method = "drawGui$lambda$25", at = @At(value = "INVOKE",
            target = "Lio/ejekta/kambrik/gui/draw/KGuiDsl;offset(IILkotlin/jvm/functions/Function1;)V"))
    private static void nh_bounties$hideScrollbar(KGuiDsl gui, int x, int y, Function1 draw) {
        if (x != SCROLLBAR_X) {
            gui.offset(x, y, draw);
        }
    }

    /** Clicks left of the visible half are outside the screen, so items held there get dropped. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return mouseX < guiLeft + SHOWN_LEFT || mouseY < guiTop
                || mouseX >= guiLeft + TEXTURE_WIDTH || mouseY >= guiTop + imageHeight;
    }
}
