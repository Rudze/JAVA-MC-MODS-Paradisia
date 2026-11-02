package com.rudze.waystonetp.teleport;

import com.rudze.waystonetp.block.WaystoneVariant;
import com.rudze.waystonetp.config.WaystoneTPConfig;
import com.rudze.waystonetp.data.WaystoneNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Handles a single waystone activation: picking a random destination of the opposite
 * variant from {@link WaystoneNetwork}, and moving the player there (any dimension).
 */
public final class WaystoneTeleportService {

    private WaystoneTeleportService() {
    }

    public static void activate(ServerLevel level, ServerPlayer player, WaystoneVariant clickedVariant, BlockPos basePos) {
        if (!WaystoneTPConfig.ENABLED.get()) {
            return;
        }

        WaystoneVariant targetVariant = clickedVariant.opposite();
        GlobalPos target = WaystoneNetwork.get(level).pickRandomTarget(clickedVariant, level.getRandom());

        MinecraftServer server = level.getServer();
        ServerLevel targetLevel = target == null ? null : server.getLevel(target.dimension());
        if (target == null || targetLevel == null) {
            player.displayClientMessage(
                    Component.translatable("message.nh_waystonetp.no_target", targetVariant.name()), true);
            return;
        }

        BlockPos standingPos = target.pos().relative(Direction.SOUTH);

        level.playSound(null, basePos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        player.teleportTo(targetLevel, standingPos.getX() + 0.5, standingPos.getY(), standingPos.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        // A cross-dimension teleport outside the normal portal path can leave the client's held item/armor
        // render desynced (player appears to have no hand/armor/held item until reconnecting or teleporting
        // again) - force a full resync of the player's equipment/inventory to the client.
        player.inventoryMenu.sendAllDataToRemote();

        targetLevel.playSound(null, standingPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
