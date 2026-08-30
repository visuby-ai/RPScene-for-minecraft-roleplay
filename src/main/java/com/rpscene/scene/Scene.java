package com.rpscene.scene;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * A persistent (or timed) roleplay scene marker anchored to a world
 * position, created via {@code /do}.
 * <p>
 * Immutable except for {@link #expirationTime}, which is only ever read
 * for expiry checks - scenes are otherwise replaced wholesale on update.
 */
public final class Scene {

    private final UUID id;
    private final UUID ownerId;
    private final String ownerName;
    private final String text;
    private final SceneType type;
    private final BlockPos pos;
    private final ResourceLocation dimension;
    private final long creationTime;
    /** -1 means the scene never expires. */
    private final long expirationTime;

    public Scene(UUID id, UUID ownerId, String ownerName, String text, SceneType type,
                 BlockPos pos, ResourceLocation dimension, long creationTime, long expirationTime) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.text = text;
        this.type = type;
        this.pos = pos;
        this.dimension = dimension;
        this.creationTime = creationTime;
        this.expirationTime = expirationTime;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getText() {
        return text;
    }

    public SceneType getType() {
        return type;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ResourceLocation getDimension() {
        return dimension;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public boolean isPersistent() {
        return expirationTime <= 0;
    }

    public boolean isExpired(long now) {
        return !isPersistent() && now >= expirationTime;
    }

    public long getRemainingMillis(long now) {
        if (isPersistent()) {
            return -1;
        }
        return Math.max(0, expirationTime - now);
    }

    // ---- Serialization ----

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("Owner", ownerId);
        tag.putString("OwnerName", ownerName);
        tag.putString("Text", text);
        tag.putString("Type", type.getId());
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        tag.putString("Dimension", dimension.toString());
        tag.putLong("CreationTime", creationTime);
        tag.putLong("ExpirationTime", expirationTime);
        return tag;
    }

    public static Scene fromNbt(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        UUID owner = tag.getUUID("Owner");
        String ownerName = tag.getString("OwnerName");
        String text = tag.getString("Text");
        SceneType type = SceneType.fromString(tag.getString("Type"));
        BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (dimension == null) {
            dimension = new ResourceLocation("minecraft", "overworld");
        }
        long creationTime = tag.getLong("CreationTime");
        long expirationTime = tag.getLong("ExpirationTime");
        return new Scene(id, owner, ownerName, text, type, pos, dimension, creationTime, expirationTime);
    }

    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUUID(ownerId);
        buf.writeUtf(ownerName, 64);
        buf.writeUtf(text, 512);
        buf.writeUtf(type.getId(), 32);
        buf.writeBlockPos(pos);
        buf.writeResourceLocation(dimension);
        buf.writeLong(creationTime);
        buf.writeLong(expirationTime);
    }

    public static Scene readFromBuffer(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        UUID owner = buf.readUUID();
        String ownerName = buf.readUtf(64);
        String text = buf.readUtf(512);
        SceneType type = SceneType.fromString(buf.readUtf(32));
        BlockPos pos = buf.readBlockPos();
        ResourceLocation dimension = buf.readResourceLocation();
        long creationTime = buf.readLong();
        long expirationTime = buf.readLong();
        return new Scene(id, owner, ownerName, text, type, pos, dimension, creationTime, expirationTime);
    }
}
