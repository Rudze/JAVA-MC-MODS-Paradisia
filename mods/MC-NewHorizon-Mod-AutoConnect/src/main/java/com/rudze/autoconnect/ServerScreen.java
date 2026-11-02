package com.rudze.autoconnect;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * Stands in for the title screen. It either connects to the server on its first tick, or, when
 * the connection failed or the player was kicked, explains why and offers to retry or quit.
 */
public final class ServerScreen extends Screen {

    public static final String SERVER_NAME = "Paradisia";
    public static final String SERVER_ADDRESS = "148.113.238.66";

    private static final int BUTTON_WIDTH = 150;

    @Nullable
    private final Component reason;
    private boolean connectOnTick;
    private MultiLineLabel reasonLabel = MultiLineLabel.EMPTY;

    private ServerScreen(Component title, @Nullable Component reason, boolean connectOnTick) {
        super(title);
        this.reason = reason;
        this.connectOnTick = connectOnTick;
    }

    public static ServerScreen connecting() {
        return new ServerScreen(Component.literal("Connexion à " + SERVER_NAME + "..."), null, true);
    }

    public static ServerScreen failed(Component title, @Nullable Component reason) {
        return new ServerScreen(title, reason, false);
    }

    @Override
    protected void init() {
        if (connectOnTick) {
            return;
        }
        if (reason != null) {
            reasonLabel = MultiLineLabel.create(font, reason, width - 50);
        }
        int y = Math.min(height / 2 + reasonLabel.getLineCount() * font.lineHeight / 2 + 20, height - 30);
        addRenderableWidget(Button.builder(Component.literal("Réessayer"), b -> connect())
                .bounds(width / 2 - BUTTON_WIDTH - 5, y, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Quitter le jeu"), b -> minecraft.stop())
                .bounds(width / 2 + 5, y, BUTTON_WIDTH, 20).build());
    }

    @Override
    public void tick() {
        if (connectOnTick) {
            connectOnTick = false;
            connect();
        }
    }

    private void connect() {
        ServerData server = new ServerData(SERVER_NAME, SERVER_ADDRESS, ServerData.Type.OTHER);
        // Cancelling the connection comes back here, ready to retry.
        ServerScreen cancelled = failed(Component.literal("Connexion annulée"), null);
        ConnectScreen.startConnecting(cancelled, minecraft, ServerAddress.parseString(SERVER_ADDRESS), server, false, null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int textTop = height / 2 - reasonLabel.getLineCount() * font.lineHeight / 2;
        graphics.drawCenteredString(font, title, width / 2, textTop - 20, 0xFFFFFF);
        reasonLabel.renderCentered(graphics, width / 2, textTop);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
