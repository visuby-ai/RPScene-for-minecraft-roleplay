package com.rpscene.network;

import com.rpscene.RPSceneMod;
import com.rpscene.network.packet.FloatingMessageClearPacket;
import com.rpscene.network.packet.FloatingMessagePacket;
import com.rpscene.network.packet.SceneRemovePacket;
import com.rpscene.network.packet.SceneSyncPacket;
import com.rpscene.network.packet.SceneUpsertPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Central packet channel for RP Scene. Uses Forge's {@link SimpleChannel}
 * with a fixed protocol version; clients and servers must match exactly,
 * which is desirable here since scene state is not optional content.
 */
public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RPSceneMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private NetworkHandler() {
    }

    /** Registered on the mod event bus; safe to call on both sides. */
    public static void registerPackets(FMLCommonSetupEvent event) {
        int id = 0;
        CHANNEL.registerMessage(id++, SceneUpsertPacket.class,
                SceneUpsertPacket::encode, SceneUpsertPacket::decode, SceneUpsertPacket::handle);
        CHANNEL.registerMessage(id++, SceneRemovePacket.class,
                SceneRemovePacket::encode, SceneRemovePacket::decode, SceneRemovePacket::handle);
        CHANNEL.registerMessage(id++, SceneSyncPacket.class,
                SceneSyncPacket::encode, SceneSyncPacket::decode, SceneSyncPacket::handle);
        CHANNEL.registerMessage(id++, FloatingMessagePacket.class,
                FloatingMessagePacket::encode, FloatingMessagePacket::decode, FloatingMessagePacket::handle);
        CHANNEL.registerMessage(id++, FloatingMessageClearPacket.class,
                FloatingMessageClearPacket::encode, FloatingMessageClearPacket::decode, FloatingMessageClearPacket::handle);
    }
}
