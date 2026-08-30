package com.rpscene.client;

import com.rpscene.RPSceneMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only mod-bus setup. Guarded with {@code value = Dist.CLIENT} so
 * Forge's annotation scanner never registers (or classloads) this on a
 * dedicated server.
 */
@Mod.EventBusSubscriber(modid = RPSceneMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RPSceneClient {

    private RPSceneClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        RPSceneMod.LOGGER.info("RP Scene client setup complete.");
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.INSPECT_SCENE);
    }
}
