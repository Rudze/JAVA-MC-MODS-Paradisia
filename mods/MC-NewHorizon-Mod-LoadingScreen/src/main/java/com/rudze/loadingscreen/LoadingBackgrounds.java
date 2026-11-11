package com.rudze.loadingscreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The images shown behind the loading screens (connecting, loading terrain...) in place of the
 * blurred panorama. A new one is picked at random each time a screen opens from in game, or at
 * startup, so it stays the same for a whole connection but changes from one to the next.
 *
 * <p>The images come from resource packs, in {@code assets/minecraft/textures/gui/backgrounds/}
 * like the Loading Backgrounds mod expects (the modpack ships Cobblemon Loading Backgrounds).
 * Every PNG there is used, drawn to cover the whole window whatever its size.
 */
public final class LoadingBackgrounds {

    private static final String FOLDER = "textures/gui/backgrounds";

    private static ResourceLocation current;
    private static int imageWidth;
    private static int imageHeight;

    @SubscribeEvent
    public void onOpening(ScreenEvent.Opening event) {
        if (event.getCurrentScreen() == null) {
            pick();
        }
    }

    private static void pick() {
        current = null;
        List<ResourceLocation> images = Minecraft.getInstance().getResourceManager()
                .listResources(FOLDER, path -> path.getPath().endsWith(".png")).keySet().stream()
                .filter(path -> path.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE))
                .toList();
        if (images.isEmpty()) {
            return;
        }
        ResourceLocation image = images.get(ThreadLocalRandom.current().nextInt(images.size()));
        // The PNG header gives the size without decoding the whole picture.
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(image)) {
            DataInputStream data = new DataInputStream(stream);
            data.skipNBytes(16);
            imageWidth = data.readInt();
            imageHeight = data.readInt();
        } catch (IOException e) {
            return;
        }
        if (imageWidth > 0 && imageHeight > 0) {
            current = image;
        }
    }

    /** @return false if there is no image to draw, so the panorama should be drawn instead */
    public static boolean render(GuiGraphics graphics) {
        if (current == null) {
            pick();
            if (current == null) {
                return false;
            }
        }
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int width = screenWidth;
        int height = width * imageHeight / imageWidth;
        if (height < screenHeight) {
            height = screenHeight;
            width = height * imageWidth / imageHeight;
        }
        graphics.blit(current, (screenWidth - width) / 2, (screenHeight - height) / 2, width, height,
                0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        return true;
    }
}
