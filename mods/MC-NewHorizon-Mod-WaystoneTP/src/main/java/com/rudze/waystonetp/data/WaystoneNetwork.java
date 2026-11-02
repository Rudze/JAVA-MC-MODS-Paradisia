package com.rudze.waystonetp.data;

import com.rudze.waystonetp.block.WaystoneVariant;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks every placed Waystone A / Waystone B position, world-wide (across dimensions).
 * Stored once on the overworld so both variant pools share a single directory.
 */
public final class WaystoneNetwork extends SavedData {

    private static final String DATA_NAME = "nh_waystonetp_network";

    public static final SavedData.Factory<WaystoneNetwork> FACTORY =
            new SavedData.Factory<>(WaystoneNetwork::new, WaystoneNetwork::load, null);

    private final Set<GlobalPos> aStones = new LinkedHashSet<>();
    private final Set<GlobalPos> bStones = new LinkedHashSet<>();

    public static WaystoneNetwork get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void register(WaystoneVariant variant, GlobalPos pos) {
        if (poolOf(variant).add(pos)) {
            setDirty();
        }
    }

    public void unregister(WaystoneVariant variant, GlobalPos pos) {
        if (poolOf(variant).remove(pos)) {
            setDirty();
        }
    }

    public boolean hasAny(WaystoneVariant variant) {
        return !poolOf(variant).isEmpty();
    }

    /** Defensive copy - lets callers prune stale entries (e.g. a dimension folder deleted by hand
     * outside the running game) without risking concurrent modification of the live set. */
    public Set<GlobalPos> positionsOf(WaystoneVariant variant) {
        return Set.copyOf(poolOf(variant));
    }

    @Nullable
    public GlobalPos pickRandomTarget(WaystoneVariant clickedVariant, RandomSource random) {
        List<GlobalPos> candidates = List.copyOf(poolOf(clickedVariant.opposite()));
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private Set<GlobalPos> poolOf(WaystoneVariant variant) {
        return variant == WaystoneVariant.A ? aStones : bStones;
    }

    private static WaystoneNetwork load(CompoundTag tag, HolderLookup.Provider provider) {
        WaystoneNetwork data = new WaystoneNetwork();
        data.aStones.addAll(readPositions(tag, "AStones"));
        data.bStones.addAll(readPositions(tag, "BStones"));
        return data;
    }

    private static List<GlobalPos> readPositions(CompoundTag tag, String key) {
        List<GlobalPos> positions = new ArrayList<>();
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            GlobalPos.CODEC.parse(NbtOps.INSTANCE, list.getCompound(i)).result().ifPresent(positions::add);
        }
        return positions;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("AStones", writePositions(aStones));
        tag.put("BStones", writePositions(bStones));
        return tag;
    }

    private static ListTag writePositions(Set<GlobalPos> positions) {
        ListTag list = new ListTag();
        for (GlobalPos pos : positions) {
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(list::add);
        }
        return list;
    }
}
