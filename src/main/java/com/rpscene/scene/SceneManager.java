package com.rpscene.scene;

import com.rpscene.network.NetworkHandler;
import com.rpscene.network.packet.SceneRemovePacket;
import com.rpscene.network.packet.SceneSyncPacket;
import com.rpscene.network.packet.SceneUpsertPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single source of truth for all active scenes on the server.
 * <p>
 * Scenes from every dimension are kept in one in-memory map (keyed by
 * UUID) for fast lookup, and persisted to the overworld's {@link
 * SceneSavedData} so a single load/save round-trip covers the whole
 * server. Expiration is checked on a slow tick in {@code
 * ForgeEventHandler} rather than per-scene timers, which keeps the
 * approach cheap even with hundreds of scenes.
 */
public final class SceneManager {

    private static final SceneManager INSTANCE = new SceneManager();

    private final Map<UUID, Scene> scenes = new ConcurrentHashMap<>();
    private boolean loaded = false;

    private SceneManager() {
    }

    public static SceneManager get() {
        return INSTANCE;
    }

    // ---- Lifecycle ----

    /** Loads all scenes from persistent storage. Call once on server start. */
    public void loadFromDisk(MinecraftServer server) {
        scenes.clear();
        ServerLevel overworld = server.overworld();
        SceneSavedData data = SceneSavedData.get(overworld);
        for (Scene scene : data.getScenes()) {
            scenes.put(scene.getId(), scene);
        }
        loaded = true;
    }

    /** Flushes the current in-memory scene set to persistent storage. */
    public void saveToDisk(MinecraftServer server) {
        if (!loaded) {
            return;
        }
        ServerLevel overworld = server.overworld();
        SceneSavedData data = SceneSavedData.get(overworld);
        data.setScenes(new ArrayList<>(scenes.values()));
    }

    public void reset() {
        scenes.clear();
        loaded = false;
    }

    // ---- Mutation ----

    public Scene create(UUID ownerId, String ownerName, String text, SceneType type,
                         BlockPos pos, ResourceLocation dimension, long durationMillis, MinecraftServer server) {
        long now = System.currentTimeMillis();
        long expiration = durationMillis > 0 ? now + durationMillis : -1;
        Scene scene = new Scene(UUID.randomUUID(), ownerId, ownerName, text, type, pos, dimension, now, expiration);
        scenes.put(scene.getId(), scene);
        saveToDisk(server);
        broadcastUpsert(scene, server);
        return scene;
    }

    /**
     * Removes a scene by id. Returns the removed scene, or null if it did
     * not exist. Caller is responsible for permission checks.
     */
    public Scene remove(UUID sceneId, MinecraftServer server) {
        Scene removed = scenes.remove(sceneId);
        if (removed != null) {
            saveToDisk(server);
            broadcastRemove(sceneId, server);
        }
        return removed;
    }

    /** Removes every scene whose expiration has passed, broadcasting removals. */
    public void tickExpirations(MinecraftServer server) {
        if (!loaded || scenes.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<UUID> expired = new ArrayList<>();
        for (Scene scene : scenes.values()) {
            if (scene.isExpired(now)) {
                expired.add(scene.getId());
            }
        }
        if (expired.isEmpty()) {
            return;
        }
        for (UUID id : expired) {
            scenes.remove(id);
            broadcastRemove(id, server);
        }
        saveToDisk(server);
    }

    // ---- Queries ----

    public Scene getById(UUID id) {
        return scenes.get(id);
    }

    public List<Scene> getAll() {
        return new ArrayList<>(scenes.values());
    }

    public List<Scene> getInDimension(ResourceLocation dimension) {
        List<Scene> result = new ArrayList<>();
        for (Scene scene : scenes.values()) {
            if (scene.getDimension().equals(dimension)) {
                result.add(scene);
            }
        }
        return result;
    }

    public List<Scene> getNear(Level level, BlockPos center, double radius) {
        List<Scene> result = new ArrayList<>();
        double radiusSq = radius * radius;
        ResourceLocation dimension = level.dimension().location();
        for (Scene scene : scenes.values()) {
            if (!scene.getDimension().equals(dimension)) {
                continue;
            }
            if (scene.getPos().distSqr(center) <= radiusSq) {
                result.add(scene);
            }
        }
        return result;
    }

    /** Nearest scene to a position within a max distance, or null. */
    public Scene getNearest(Level level, BlockPos center, double maxDistance) {
        Scene best = null;
        double bestDistSq = maxDistance * maxDistance;
        ResourceLocation dimension = level.dimension().location();
        for (Scene scene : scenes.values()) {
            if (!scene.getDimension().equals(dimension)) {
                continue;
            }
            double distSq = scene.getPos().distSqr(center);
            if (distSq <= bestDistSq) {
                best = scene;
                bestDistSq = distSq;
            }
        }
        return best;
    }

    // ---- Networking ----

    public void broadcastUpsert(Scene scene, MinecraftServer server) {
        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new SceneUpsertPacket(scene));
    }

    public void broadcastRemove(UUID sceneId, MinecraftServer server) {
        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new SceneRemovePacket(sceneId));
    }

    /** Sends the full current scene set to a single player, e.g. on join. */
    public void syncToPlayer(ServerPlayer player) {
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SceneSyncPacket(getAll()));
    }
}
