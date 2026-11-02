package com.rudze.autoconnect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Swaps out the vanilla menus that lead anywhere but the server:
 * <ul>
 *   <li>the title screen becomes {@link ServerScreen} connecting straight away;</li>
 *   <li>being kicked, or failing to connect, shows {@link ServerScreen} with the reason, so the
 *       player can retry or quit;</li>
 *   <li>the pause menu's disconnect button reads "Quitter le jeu": vanilla then sends the player
 *       to the server list, which we turn into closing the game.</li>
 * </ul>
 */
public final class ScreenRedirects {

    private static final Component QUIT = Component.literal("Quitter le jeu");

    @SubscribeEvent
    public void onOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen) {
            event.setNewScreen(ServerScreen.connecting());
        } else if (event.getNewScreen() instanceof DisconnectedScreen disconnected) {
            event.setNewScreen(ServerScreen.failed(disconnected.getTitle(), disconnected.details.reason()));
        } else if (event.getNewScreen() instanceof JoinMultiplayerScreen) {
            // Only reachable by leaving the server from the pause menu.
            event.setCanceled(true);
            Minecraft.getInstance().stop();
        }
    }

    @SubscribeEvent
    public void onPauseInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof PauseScreen pause && pause.disconnectButton != null) {
            pause.disconnectButton.setMessage(QUIT);
        }
    }
}
