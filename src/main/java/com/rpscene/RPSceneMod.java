package com.rpscene;

import com.rpscene.event.ForgeEventHandler;
import com.rpscene.event.ModEventHandler;
import com.rpscene.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * RP Scene - Persistent roleplay context and investigation tools.
 * <p>
 * Provides /me floating action text, /do persistent world-anchored scene
 * markers with typed icons and optional expiration, an inspection UI, and
 * full server/client synchronization backed by SavedData persistence.
 */
@Mod(RPSceneMod.MOD_ID)
public class RPSceneMod {

    public static final String MOD_ID = "rpscene";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    /** Data storage sub-folder key, mirrors vanilla LevelResource pattern for reference only. */
    public static final String SCENE_DATA_NAME = "rpscene_data";

    public RPSceneMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        Config.register();

        modEventBus.addListener(NetworkHandler::registerPackets);

        MinecraftForge.EVENT_BUS.addListener(ModEventHandler::onRegisterCommands);
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());

        // Client-only classes (com.rpscene.client.*) are registered purely
        // through @Mod.EventBusSubscriber(value = Dist.CLIENT) annotations,
        // which Forge's ASM annotation scan can filter by side without ever
        // classloading them on a dedicated server. Nothing to do here.
    }
}
