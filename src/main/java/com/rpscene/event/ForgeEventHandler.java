package com.rpscene.event;

import com.rpscene.scene.SceneManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Server-side lifecycle and tick handling.
 * <p>
 * Expiration is checked once per second (every 20 ticks) rather than
 * every tick - scene expiry does not need tick-perfect accuracy and this
 * keeps the cost negligible even with hundreds of scenes.
 */
public class ForgeEventHandler {

    private static final int EXPIRATION_CHECK_INTERVAL_TICKS = 20;
    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        SceneManager.get().loadFromDisk(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        SceneManager.get().saveToDisk(event.getServer());
        SceneManager.get().reset();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tickCounter++;
        if (tickCounter < EXPIRATION_CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            SceneManager.get().tickExpirations(server);
        }
    }

    /**
     * Synchronizes the full current scene set to a player as soon as they
     * log in, so players who arrive later immediately see ongoing or past
     * roleplay context without asking other players - the core design goal
     * of the mod.
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SceneManager.get().syncToPlayer(player);
        }
    }
}
