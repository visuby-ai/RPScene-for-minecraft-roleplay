package com.rpscene.network.packet;

import com.rpscene.FloatingMessageChannel;
import com.rpscene.client.ClientFloatingMessageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent server -> client to display a floating text line above a
 * player's head - covers {@code /me}, {@code /do}, and {@code /ooc}
 * alike, distinguished by {@link FloatingMessageChannel}. Only sent to
 * players within the relevant range of the actor, so the client does
 * not need to re-filter by distance (though it still fades based on its
 * own camera distance for smooth visuals).
 */
public class FloatingMessagePacket {

    private final int entityId;
    private final byte channelId;
    private final String text;
    private final int durationSeconds;

    public FloatingMessagePacket(int entityId, FloatingMessageChannel channel, String text, int durationSeconds) {
        this.entityId = entityId;
        this.channelId = channel.getId();
        this.text = text;
        this.durationSeconds = durationSeconds;
    }

    public static void encode(FloatingMessagePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeByte(packet.channelId);
        buf.writeUtf(packet.text, 256);
        buf.writeVarInt(packet.durationSeconds);
    }

    public static FloatingMessagePacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        byte channelId = buf.readByte();
        String text = buf.readUtf(256);
        int duration = buf.readVarInt();
        return new FloatingMessagePacket(entityId, FloatingMessageChannel.byId(channelId), text, duration);
    }

    public static void handle(FloatingMessagePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientFloatingMessageManager.get().addMessage(
                packet.entityId, packet.text, FloatingMessageChannel.byId(packet.channelId), packet.durationSeconds));
        ctx.setPacketHandled(true);
    }
}
