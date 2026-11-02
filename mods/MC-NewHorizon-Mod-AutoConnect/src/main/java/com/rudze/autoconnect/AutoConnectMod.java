package com.rudze.autoconnect;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Turns the client into a launcher for the Paradisia server: no title screen, no server list.
 * The game joins the server as soon as it starts, and leaving the server closes the game.
 */
@Mod(value = AutoConnectMod.MODID, dist = Dist.CLIENT)
public final class AutoConnectMod {

    public static final String MODID = "nh_autoconnect";

    public AutoConnectMod() {
        NeoForge.EVENT_BUS.register(new ScreenRedirects());
    }
}
