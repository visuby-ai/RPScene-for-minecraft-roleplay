package com.rpscene.network.packet;

import com.rpscene.client.ClientSceneManager;
import com.rpscene.scene.Scene;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Sent server -> client with the complete current scene set. Used on
 * player join so late arrivals immediately see all existing roleplay
 * context without waiting for individual upserts.
 */
public class SceneSyncPacket {

    private final List<Scene> scenes;

    public SceneSyncPacket(List<Scene> scenes) {
        this.scenes = scenes;
    }

    public static void encode(SceneSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.scenes.size());
        for (Scene scene : packet.scenes) {
            scene.writeToBuffer(buf);
        }
    }

    public static SceneSyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Scene> scenes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            scenes.add(Scene.readFromBuffer(buf));
        }
        return new SceneSyncPacket(scenes);
    }

    public static void handle(SceneSyncPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientSceneManager.get().replaceAll(packet.scenes));
        ctx.setPacketHandled(true);
    }
}
