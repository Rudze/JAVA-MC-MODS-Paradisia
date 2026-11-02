package tallestred.numismaticoverhaul.client.gui;

import io.wispforest.owo.ui.base.BaseUIModelHandledScreen;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.block.PiggyBankScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PiggyBankScreen extends BaseUIModelHandledScreen<FlowLayout, PiggyBankScreenHandler> {

    private TextureComponent bronzeHint, silverHint, goldHint;

    public PiggyBankScreen(PiggyBankScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, FlowLayout.class, BaseUIModelScreen.DataSource.asset(ResourceLocation.fromNamespaceAndPath(NumismaticOverhaul.MODID, "piggy_bank")));
        this.imageHeight = 145;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = (this.imageWidth - Minecraft.getInstance().font.width(title)) / 2;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.bronzeHint = this.uiAdapter.rootComponent.childById(TextureComponent.class, "bronze-hint");
        this.silverHint = this.uiAdapter.rootComponent.childById(TextureComponent.class, "silver-hint");
        this.goldHint = this.uiAdapter.rootComponent.childById(TextureComponent.class, "gold-hint");
    }

    @Override
    public void render(GuiGraphics matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        this.bronzeHint.sizing(this.menu.getSlot(0).hasItem() ? Sizing.fixed(0) : Sizing.fixed(16));
        this.silverHint.sizing(this.menu.getSlot(1).hasItem() ? Sizing.fixed(0) : Sizing.fixed(16));
        this.goldHint.sizing(this.menu.getSlot(2).hasItem() ? Sizing.fixed(0) : Sizing.fixed(16));
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

    }
}
