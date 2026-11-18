package com.rudze.api;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

/**
 * Serves a read-only JSON API about the players of the server, for the Paradisia website.
 * See {@link ApiServer} for the endpoints.
 */
@Mod(ApiMod.MODID)
public final class ApiMod {

    public static final String MODID = "nh_api";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static ApiServer server;

    public ApiMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ApiConfig.SPEC, "nh_api.toml");
        NeoForge.EVENT_BUS.addListener(ApiMod::onServerStarted);
        NeoForge.EVENT_BUS.addListener(ApiMod::onServerStopping);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        if (!ApiConfig.ENABLED.get()) {
            return;
        }
        try {
            server = new ApiServer(event.getServer());
            server.start(ApiConfig.BIND_ADDRESS.get(), ApiConfig.PORT.get());
        } catch (Exception e) {
            LOGGER.error("Could not start the web API", e);
            server = null;
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
}
