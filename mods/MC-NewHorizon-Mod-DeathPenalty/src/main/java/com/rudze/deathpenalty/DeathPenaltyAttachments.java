package com.rudze.deathpenalty;

import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Server-authoritative, disk-persisted per-player state for the death penalty respawn delay.
 * Storing this as an attachment (rather than a plain in-memory map) means the cooldown survives
 * disconnect/reconnect and full server restarts - a player cannot escape it by logging off.
 */
public final class DeathPenaltyAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, DeathPenaltyMod.MODID);

    /** Wall-clock epoch millis (System.currentTimeMillis()) at which the respawn penalty ends. 0 = none active. */
    public static final Supplier<AttachmentType<Long>> RESPAWN_PENALTY_END = ATTACHMENTS.register(
            "respawn_penalty_end",
            () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG)
                    .copyOnDeath()
                    .build());

    private DeathPenaltyAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
