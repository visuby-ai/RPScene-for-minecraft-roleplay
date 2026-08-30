package com.rpscene.network.packet;

import com.rpscene.client.ClientSceneManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Sent server -> client when a scene is deleted or has expired. */
public class SceneRemovePacket {

    private final UUID sceneId;

    public SceneRemovePacket(UUID sceneId) {
        this.sceneId = sceneId;
    }

    public static void encode(SceneRemovePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.sceneId);
    }

    public static SceneRemovePacket decode(FriendlyByteBuf buf) {
        return new SceneRemovePacket(buf.readUUID());
    }

    public static void handle(SceneRemovePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientSceneManager.get().remove(packet.sceneId));
        ctx.setPacketHandled(true);
    }
}
