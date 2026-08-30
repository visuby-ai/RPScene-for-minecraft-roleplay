package com.rpscene.network.packet;

import com.rpscene.client.ClientSceneManager;
import com.rpscene.scene.Scene;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent server -> client whenever a scene is created (or, in a future
 * update, edited). The client simply replaces its cached copy.
 */
public class SceneUpsertPacket {

    private final Scene scene;

    public SceneUpsertPacket(Scene scene) {
        this.scene = scene;
    }

    public static void encode(SceneUpsertPacket packet, FriendlyByteBuf buf) {
        packet.scene.writeToBuffer(buf);
    }

    public static SceneUpsertPacket decode(FriendlyByteBuf buf) {
        return new SceneUpsertPacket(Scene.readFromBuffer(buf));
    }

    public static void handle(SceneUpsertPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientSceneManager.get().upsert(packet.scene));
        ctx.setPacketHandled(true);
    }
}
