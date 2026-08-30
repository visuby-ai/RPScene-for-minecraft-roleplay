package com.rpscene.network.packet;

import com.rpscene.FloatingMessageChannel;
import com.rpscene.client.ClientFloatingMessageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent server -> client to immediately clear every currently active
 * message of one channel above one entity's head, ahead of its natural
 * expiration. Used by {@code /ooc remove} to let a player take back
 * their own out-of-character message early.
 */
public class FloatingMessageClearPacket {

    private final int entityId;
    private final byte channelId;

    public FloatingMessageClearPacket(int entityId, FloatingMessageChannel channel) {
        this.entityId = entityId;
        this.channelId = channel.getId();
    }

    public static void encode(FloatingMessageClearPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeByte(packet.channelId);
    }

    public static FloatingMessageClearPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        byte channelId = buf.readByte();
        return new FloatingMessageClearPacket(entityId, FloatingMessageChannel.byId(channelId));
    }

    public static void handle(FloatingMessageClearPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientFloatingMessageManager.get()
                .clearChannel(packet.entityId, FloatingMessageChannel.byId(packet.channelId)));
        ctx.setPacketHandled(true);
    }
}
