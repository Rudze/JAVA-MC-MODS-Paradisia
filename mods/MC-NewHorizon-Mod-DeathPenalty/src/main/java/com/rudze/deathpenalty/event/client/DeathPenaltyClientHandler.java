package com.rudze.deathpenalty.event.client;

import com.rudze.deathpenalty.config.DeathPenaltyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Client-only UX gate: greys out the death screen's Respawn button and shows a countdown on it
 * for {@code respawnDelaySeconds}. Purely visual - the actual stat penalty and early-respawn
 * lockout debuff are applied server-side in
 * {@link com.rudze.deathpenalty.event.DeathPenaltyServerHandler} regardless
 * of how/when the player actually respawns.
 *
 * <p>Vanilla's death screen has its own bypass of the disabled Respawn button: clicking
 * "Title Screen" (outside hardcore) opens a {@link ConfirmScreen} ("deathScreen.quit.confirm")
 * whose "no" option is itself labelled "Respawn" and calls {@code requestRespawn()} directly,
 * with no cooldown check at all. Since the point of that dialog is only to offer a same-labelled
 * escape hatch, we skip it entirely: the instant it appears, we press its confirm ("Title Screen")
 * button on the player's behalf, so clicking "Title Screen" once just disconnects, like hardcore
 * mode already does.
 */
public final class DeathPenaltyClientHandler {

    private static final String RESPAWN_KEY = "deathScreen.respawn";
    private static final String TITLE_SCREEN_KEY = "deathScreen.titleScreen";
    private static final String QUIT_CONFIRM_TITLE_KEY = "deathScreen.quit.confirm";

    private long clientTicks = 0;
    private Screen trackedDeathScreen = null;
    private long respawnAllowedAtTick = -1;

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        clientTicks++;
        if (trackedDeathScreen == null) {
            return;
        }
        if (Minecraft.getInstance().screen != trackedDeathScreen) {
            trackedDeathScreen = null;
            return;
        }
        updateRespawnButton(trackedDeathScreen);
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (!DeathPenaltyConfig.ENABLED.get()) {
            return;
        }

        if (event.getScreen() instanceof DeathScreen deathScreen) {
            if (!DeathPenaltyConfig.GREY_OUT_RESPAWN_BUTTON.get()) {
                return;
            }
            if (trackedDeathScreen != deathScreen) {
                trackedDeathScreen = deathScreen;
                respawnAllowedAtTick = clientTicks + DeathPenaltyConfig.RESPAWN_DELAY_SECONDS.get() * 20L;
            }
            updateRespawnButton(deathScreen);
            return;
        }

        if (event.getScreen() instanceof ConfirmScreen confirmScreen && isDeathQuitConfirmation(confirmScreen)) {
            Button confirmButton = findButtonByKey(confirmScreen, TITLE_SCREEN_KEY);
            if (confirmButton != null) {
                confirmButton.onPress();
            }
        }
    }

    private static boolean isDeathQuitConfirmation(Screen screen) {
        return screen.getTitle().getContents() instanceof TranslatableContents translatable
                && QUIT_CONFIRM_TITLE_KEY.equals(translatable.getKey());
    }

    private void updateRespawnButton(Screen screen) {
        Button respawnButton = findButtonByKey(screen, RESPAWN_KEY);
        if (respawnButton == null) {
            return;
        }

        long remainingTicks = respawnAllowedAtTick - clientTicks;
        if (remainingTicks <= 0) {
            respawnButton.active = true;
            respawnButton.setMessage(Component.translatable(RESPAWN_KEY));
            trackedDeathScreen = null;
        } else {
            respawnButton.active = false;
            respawnButton.setMessage(Component.translatable(RESPAWN_KEY)
                    .copy()
                    .append(Component.literal(" (" + formatDuration(remainingTicks) + ")")));
        }
    }

    private static Button findButtonByKey(Screen screen, String translationKey) {
        for (var listener : screen.children()) {
            if (listener instanceof Button button
                    && button.getMessage().getContents() instanceof TranslatableContents translatable
                    && translationKey.equals(translatable.getKey())) {
                return button;
            }
        }
        return null;
    }

    private static String formatDuration(long ticks) {
        long totalSeconds = (ticks + 19) / 20;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
