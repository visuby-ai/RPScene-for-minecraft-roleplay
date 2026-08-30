package com.rpscene.event;

import com.rpscene.command.DoCommand;
import com.rpscene.command.MeCommand;
import com.rpscene.command.OocCommand;
import com.rpscene.command.SceneCommand;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * Command registration on the Forge event bus. Safe to load on both
 * client and dedicated server - {@code RegisterCommandsEvent} and
 * Brigadier are common-side types. Client-only setup (key mappings,
 * client setup event) lives in {@code com.rpscene.client.RPSceneClient}
 * instead, guarded by {@code @Mod.EventBusSubscriber(value = Dist.CLIENT)}
 * so its client-only event types are never touched on a dedicated server.
 */
public final class ModEventHandler {

    private ModEventHandler() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MeCommand.register(event.getDispatcher());
        DoCommand.register(event.getDispatcher());
        OocCommand.register(event.getDispatcher());
        SceneCommand.register(event.getDispatcher());
    }
}
