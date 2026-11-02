package com.rudze.quests.data;

import com.mojang.serialization.Codec;
import com.rudze.quests.QuestsMod;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Per-player story flags, saved with the player and kept on death. Future quest state belongs
 * here too.
 */
public final class PlayerProgress {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, QuestsMod.MODID);

    /** Set once the player has heard Professor Chen's welcome speech through to the end. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> CHEN_INTRO_DONE =
            ATTACHMENT_TYPES.register("chen_intro_done",
                    () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    private PlayerProgress() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static boolean chenIntroDone(Player player) {
        return player.getData(CHEN_INTRO_DONE);
    }

    public static void setChenIntroDone(Player player) {
        player.setData(CHEN_INTRO_DONE, true);
    }
}
