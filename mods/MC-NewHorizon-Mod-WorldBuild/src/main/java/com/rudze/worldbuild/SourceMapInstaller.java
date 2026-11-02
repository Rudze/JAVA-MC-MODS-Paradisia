package com.rudze.worldbuild;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Before the server touches any dimension storage, copies the premade Driftfall Island map
 * (kept outside the mod jar, in {@code <gameDir>/nh_worldbuild_source/}) into world_build's
 * region/entities/poi folders, once. After that, Minecraft simply loads those chunks from disk
 * instead of generating fresh ones, exactly like any other already-explored area.
 */
public final class SourceMapInstaller {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceMapInstaller.class);
    private static final String SOURCE_FOLDER_NAME = "nh_worldbuild_source";

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        Path source = sourceDir();
        if (!Files.isDirectory(source)) {
            LOGGER.warn("[nh_worldbuild] Source map folder not found at {}; world_build will generate as plain terrain.", source);
            return;
        }

        Path target = DimensionType.getStorageFolder(WorldBuildMod.WORLD_BUILD_LEVEL, event.getServer().getWorldPath(LevelResource.ROOT));
        if (isAlreadyInstalled(target)) {
            return;
        }

        try {
            copyDirectory(source.resolve("region"), target.resolve("region"));
            copyIfPresent(source.resolve("entities"), target.resolve("entities"));
            copyIfPresent(source.resolve("poi"), target.resolve("poi"));
            LOGGER.info("[nh_worldbuild] Installed the Driftfall Island source map into {}.", target);
        } catch (IOException e) {
            LOGGER.error("[nh_worldbuild] Failed to install the source map into {}.", target, e);
        }
    }

    static Path sourceDir() {
        return FMLPaths.GAMEDIR.get().resolve(SOURCE_FOLDER_NAME);
    }

    /**
     * Reads SpawnX/SpawnZ straight out of the source map's own level.dat (the actual dimension
     * storage we copy from has no level.dat of its own).
     */
    static Optional<BlockPos> readSourceSpawnXZ() {
        Path levelDat = sourceDir().resolve("level.dat");
        if (!Files.isRegularFile(levelDat)) {
            return Optional.empty();
        }
        try {
            CompoundTag root = NbtIo.readCompressed(levelDat, NbtAccounter.unlimitedHeap());
            CompoundTag data = root.getCompound("Data");
            return Optional.of(new BlockPos(data.getInt("SpawnX"), 0, data.getInt("SpawnZ")));
        } catch (IOException e) {
            LOGGER.error("[nh_worldbuild] Failed to read spawn position from {}.", levelDat, e);
            return Optional.empty();
        }
    }

    private static boolean isAlreadyInstalled(Path target) {
        Path region = target.resolve("region");
        if (!Files.isDirectory(region)) {
            return false;
        }
        try (Stream<Path> entries = Files.list(region)) {
            return entries.findAny().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    private static void copyIfPresent(Path source, Path target) throws IOException {
        if (Files.isDirectory(source)) {
            copyDirectory(source, target);
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                Path dest = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
