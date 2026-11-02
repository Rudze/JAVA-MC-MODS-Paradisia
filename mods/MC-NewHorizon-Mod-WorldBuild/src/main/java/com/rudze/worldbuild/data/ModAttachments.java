package com.rudze.worldbuild.data;

import com.rudze.worldbuild.WorldBuildMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, WorldBuildMod.MODID);

    /** Which non-Nether dimension an entity most recently entered the Nether from, so a portal
     * taken back out of the Nether can return them there instead of always landing in the
     * overworld (vanilla only ever remembers "the overworld" as the Nether's other side). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ResourceKey<Level>>> LAST_NON_NETHER_DIMENSION =
            ATTACHMENT_TYPES.register(
                    "last_non_nether_dimension",
                    () -> AttachmentType.builder(() -> Level.OVERWORLD)
                            .serialize(ResourceKey.codec(Registries.DIMENSION))
                            .build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
