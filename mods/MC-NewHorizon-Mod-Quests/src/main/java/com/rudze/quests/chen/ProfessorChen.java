package com.rudze.quests.chen;

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
import java.util.UUID;

/**
 * Professor Chen is a single Cobblemon NPC (class {@code nh_quests:professeur_chen}) whose UUID is
 * remembered here, so {@code /chen} can move him instead of spawning a second one.
 *
 * <p>If he sits in an unloaded chunk he can't be moved, so a fresh one is spawned and the saved
 * UUID switched to it; {@link ChenInteractionHandler} then drops the stale copy whenever its chunk
 * loads again.
 */
public final class ProfessorChen extends SavedData {

    public static final String TAG = QuestsMod.MODID + ".professeur_chen";
    public static final ResourceLocation NPC_CLASS = ResourceLocation.fromNamespaceAndPath(QuestsMod.MODID, "professeur_chen");
    public static final String NAME = "Professeur Chen";

    private static final String SKIN = "/nh_quests/chen_skin.png";
    private static final String DATA_NAME = "nh_quests_professeur_chen";
    private static final SavedData.Factory<ProfessorChen> FACTORY =
            new SavedData.Factory<>(ProfessorChen::new, ProfessorChen::load, null);

    @Nullable
    private UUID uuid;

    public static ProfessorChen get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static boolean isChen(Entity entity) {
        return entity instanceof NPCEntity && entity.getTags().contains(TAG);
    }

    public boolean isCurrent(Entity entity) {
        return entity.getUUID().equals(uuid);
    }

    @Nullable
    public NPCEntity findLoaded(MinecraftServer server) {
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

    /** @return true if an existing Chen was moved, false if a new one was spawned */
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

        NPCClass npcClass = NPCClasses.getByIdentifier(NPC_CLASS);
        if (npcClass == null) {
            throw new IllegalStateException("NPC class " + NPC_CLASS + " isn't loaded; is the nh_quests datapack missing?");
        }
        NPCEntity npc = new NPCEntity(level);
        npc.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
        npc.setYHeadRot(player.getYRot());
        npc.setNpc(npcClass);
        npc.initialize(1);
        npc.setCustomName(Component.literal(NAME));
        npc.setInvulnerable(Boolean.TRUE);
        npc.setLeashable(Boolean.FALSE);
        npc.addTag(TAG);
        applySkin(npc);

        // Saved before the entity joins the level so the stale-copy check already knows about it.
        uuid = npc.getUUID();
        setDirty();
        level.addFreshEntity(npc);
        return moved;
    }

    /**
     * Gives Chen the skin bundled in this jar ({@code nh_quests/chen_skin.png}). Cobblemon syncs the
     * texture bytes to clients and saves them with the entity, so no client mod is needed.
     * {@link NPCEntity#loadTexture} only takes a URI it can open itself, hence the temp file.
     */
    private static void applySkin(NPCEntity npc) {
        try (InputStream in = ProfessorChen.class.getResourceAsStream(SKIN)) {
            if (in == null) {
                throw new IOException(SKIN + " is missing from the jar");
            }
            Path file = Files.createTempFile("nh_quests_chen", ".png");
            try {
                Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
                npc.loadTexture(file.toUri(), NPCPlayerModelType.DEFAULT);
            } finally {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            QuestsMod.LOGGER.error("Couldn't apply Professor Chen's skin", e);
        }
    }

    private static ProfessorChen load(CompoundTag tag, HolderLookup.Provider provider) {
        ProfessorChen data = new ProfessorChen();
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
