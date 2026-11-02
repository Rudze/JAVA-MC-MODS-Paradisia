package com.rudze.worldbuild;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Rudzee, mayor of Paradisia: the only player allowed to build anywhere in world_build, hand out
 * claims and run the admin commands of this mod and nh_quests.
 *
 * <p>Matched by UUID rather than name since the server runs in online mode and a Mojang
 * name change would otherwise silently strip (or hand over) every mayor permission.
 */
public final class Mayor {

    public static final UUID MAYOR_UUID = UUID.fromString("a5ff35d0-340d-4e0d-9f18-e851557b5b75");

    private Mayor() {
    }

    public static boolean isMayor(Player player) {
        return player != null && MAYOR_UUID.equals(player.getUUID());
    }

    public static boolean isMayor(CommandSourceStack source) {
        return isMayor(source.getPlayer());
    }
}
