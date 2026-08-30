package com.rpscene.client;

import com.rpscene.scene.Scene;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side mirror of the server's active scene set, kept up to date via
 * {@code SceneUpsertPacket}, {@code SceneRemovePacket}, and
 * {@code SceneSyncPacket}. Used by both the world renderer and the F-key
 * inspection raycast.
 */
public final class ClientSceneManager {

    private static final ClientSceneManager INSTANCE = new ClientSceneManager();

    private final Map<UUID, Scene> scenes = new ConcurrentHashMap<>();

    private ClientSceneManager() {
    }

    public static ClientSceneManager get() {
        return INSTANCE;
    }

    public void upsert(Scene scene) {
        scenes.put(scene.getId(), scene);
    }

    public void remove(UUID id) {
        scenes.remove(id);
    }

    public void replaceAll(List<Scene> newScenes) {
        scenes.clear();
        for (Scene scene : newScenes) {
            scenes.put(scene.getId(), scene);
        }
    }

    public void clear() {
        scenes.clear();
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

    /** Nearest scene to a position within a max distance, in the given dimension, or null. */
    public Scene getNearest(ResourceLocation dimension, BlockPos center, double maxDistance) {
        Scene best = null;
        double bestDistSq = maxDistance * maxDistance;
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
}
