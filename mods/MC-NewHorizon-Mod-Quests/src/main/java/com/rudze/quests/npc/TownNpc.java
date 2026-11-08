package com.rudze.quests.npc;

import com.cobblemon.mod.common.api.npc.NPCClass;
import com.cobblemon.mod.common.api.npc.NPCClasses;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.entity.npc.NPCPlayerModelType;
import com.rudze.quests.QuestsMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A one-of-a-kind townsfolk: a Cobblemon NPC (class {@code nh_quests:<id>}) whose UUID is
 * remembered in the overworld's saved data, so its placement command moves it instead of spawning
 * a second one.
 *
 * <p>If it sits in an unloaded chunk it can't be moved, so a fresh one is spawned and the saved
 * UUID switched to it; {@link NpcInteractionHandler} then drops the stale copy whenever its chunk
 * loads again.
 */
public final class TownNpc {

    public static final TownNpc CHEN = new TownNpc("professeur_chen", "Professeur Chen", "chen_skin.png");
    public static final TownNpc TOM = new TownNpc("tom", "Tom", "tom_skin.png");
    public static final List<TownNpc> ALL = List.of(CHEN, TOM);

    private final String tag;
    private final ResourceLocation npcClass;
    private final String name;
    private final String skin;
    private final SavedData.Factory<Data> factory = new SavedData.Factory<>(Data::new, Data::load, null);
    private final String dataName;

    private TownNpc(String id, String name, String skin) {
        this.tag = QuestsMod.MODID + "." + id;
        this.npcClass = ResourceLocation.fromNamespaceAndPath(QuestsMod.MODID, id);
        this.name = name;
        this.skin = "/" + QuestsMod.MODID + "/" + skin;
        this.dataName = QuestsMod.MODID + "_" + id;
    }

    public String name() {
        return name;
    }

    public static Optional<TownNpc> of(Entity entity) {
        if (entity instanceof NPCEntity) {
            for (TownNpc npc : ALL) {
                if (entity.getTags().contains(npc.tag)) {
                    return Optional.of(npc);
                }
            }
        }
        return Optional.empty();
    }

    public boolean isCurrent(MinecraftServer server, Entity entity) {
        return entity.getUUID().equals(data(server).uuid);
    }

    @Nullable
    public NPCEntity findLoaded(MinecraftServer server) {
        UUID uuid = data(server).uuid;
        if (uuid == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(uuid) instanceof NPCEntity npc) {
                return npc;
            }
        }
        return null;
    }

    /** @return true if an existing copy was moved, false if a new one was spawned */
    public boolean moveOrSpawnAt(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        NPCEntity existing = findLoaded(player.server);
        if (existing != null && existing.level() == level) {
            existing.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
            existing.setYHeadRot(player.getYRot());
            existing.setYBodyRot(player.getYRot());
            existing.getNavigation().stop();
            applySkin(existing);
            return true;
        }
        boolean moved = existing != null;
        if (existing != null) {
            existing.discard();
        }

        NPCClass cls = NPCClasses.getByIdentifier(npcClass);
        if (cls == null) {
            throw new IllegalStateException("NPC class " + npcClass + " isn't loaded; is the nh_quests datapack missing?");
        }
        NPCEntity npc = new NPCEntity(level);
        npc.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
        npc.setYHeadRot(player.getYRot());
        npc.setNpc(cls);
        npc.initialize(1);
        npc.setCustomName(Component.literal(name));
        npc.setInvulnerable(Boolean.TRUE);
        npc.setLeashable(Boolean.FALSE);
        npc.addTag(tag);
        applySkin(npc);

        // Saved before the entity joins the level so the stale-copy check already knows about it.
        Data data = data(player.server);
        data.uuid = npc.getUUID();
        data.setDirty();
        level.addFreshEntity(npc);
        return moved;
    }

    /**
     * Gives the NPC the skin bundled in this jar. Cobblemon syncs the texture bytes to clients and
     * saves them with the entity, so no client mod is needed. {@link NPCEntity#loadTexture} only
     * takes a URI it can open itself, hence the temp file.
     */
    private void applySkin(NPCEntity npc) {
        try (InputStream in = TownNpc.class.getResourceAsStream(skin)) {
            if (in == null) {
                throw new IOException(skin + " is missing from the jar");
            }
            Path file = Files.createTempFile(dataName, ".png");
            try {
                Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
                npc.loadTexture(file.toUri(), NPCPlayerModelType.DEFAULT);
            } finally {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            QuestsMod.LOGGER.error("Couldn't apply {}'s skin", name, e);
        }
    }

    private Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory, dataName);
    }

    private static final class Data extends SavedData {

        @Nullable
        private UUID uuid;

        private static Data load(CompoundTag tag, HolderLookup.Provider provider) {
            Data data = new Data();
            if (tag.hasUUID("Uuid")) {
                data.uuid = tag.getUUID("Uuid");
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            if (uuid != null) {
                tag.putUUID("Uuid", uuid);
            }
            return tag;
        }
    }
}
