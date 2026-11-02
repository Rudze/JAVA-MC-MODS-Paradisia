package com.rudze.loadingscreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The images shown behind the loading screens (connecting, loading terrain...) in place of the
 * blurred panorama. A new one is picked at random each time a screen opens from in game, or at
 * startup, so it stays the same for a whole connection but changes from one to the next.
 *
 * <p>Every PNG in {@code assets/nh_loadingscreen/textures/gui/backgrounds/} is used; they are
 * 16:9 and drawn to cover the whole window.
 */
public final class LoadingBackgrounds {

    private static final String FOLDER = "textures/gui/backgrounds";
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private static ResourceLocation current;

    @SubscribeEvent
    public void onOpening(ScreenEvent.Opening event) {
        if (event.getCurrentScreen() == null) {
            current = pick();
        }
    }

    private static ResourceLocation pick() {
        List<ResourceLocation> images = Minecraft.getInstance().getResourceManager()
                .listResources(FOLDER, path -> path.getPath().endsWith(".png")).keySet().stream()
                .filter(path -> path.getNamespace().equals(LoadingScreenMod.MODID))
                .toList();
        return images.isEmpty() ? null : images.get(ThreadLocalRandom.current().nextInt(images.size()));
    }

    /** @return false if there is no image to draw, so the panorama should be drawn instead */
    public static boolean render(GuiGraphics graphics) {
        if (current == null) {
            current = pick();
            if (current == null) {
                return false;
            }
        }
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int width = screenWidth;
        int height = width * HEIGHT / WIDTH;
        if (height < screenHeight) {
            height = screenHeight;
            width = height * WIDTH / HEIGHT;
        }
        graphics.blit(current, (screenWidth - width) / 2, (screenHeight - height) / 2, width, height, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        return true;
    }
}
