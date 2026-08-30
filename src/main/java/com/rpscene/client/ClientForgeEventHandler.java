package com.rpscene.client;

import com.rpscene.Config;
import com.rpscene.RPSceneMod;
import com.rpscene.client.screen.SceneDetailsScreen;
import com.rpscene.scene.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles the scene inspection key ("Look at scene, press F" per the
 * spec) and clears client-side caches on disconnect so stale scenes and
 * /me actions don't linger into the next session or singleplayer world.
 */
@Mod.EventBusSubscriber(modid = RPSceneMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEventHandler {

    private ClientForgeEventHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!KeyBindings.INSPECT_SCENE.consumeClick()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (mc.screen != null) {
            // Don't hijack the key while another screen (chat, inventory, etc.) is open.
            return;
        }

        BlockPos playerPos = mc.player.blockPosition();
        double range = Config.SCENE_INSPECT_RANGE.get();
        Scene nearest = ClientSceneManager.get().getNearest(
                mc.level.dimension().location(), playerPos, range);

        if (nearest != null) {
            mc.setScreen(new SceneDetailsScreen(nearest));
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSceneManager.get().clear();
        ClientFloatingMessageManager.get().clear();
    }
}
