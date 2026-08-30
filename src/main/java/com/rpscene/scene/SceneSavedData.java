package com.rpscene.scene;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-dimension persistence for {@link Scene} objects, stored under
 * {@code data/rpscene_data.dat} in each dimension's storage folder.
 * <p>
 * Actual in-memory ownership and cross-dimension bookkeeping lives in
 * {@link SceneManager}; this class only handles the NBT round-trip so the
 * manager can be the single source of truth for reads/writes/expiration.
 */
public class SceneSavedData extends SavedData {

    public static final String DATA_NAME = "rpscene_data";

    private final List<Scene> scenes = new ArrayList<>();

    public SceneSavedData() {
    }

    public List<Scene> getScenes() {
        return scenes;
    }

    public void setScenes(List<Scene> newScenes) {
        this.scenes.clear();
        this.scenes.addAll(newScenes);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Scene scene : scenes) {
            list.add(scene.toNbt());
        }
        tag.put("Scenes", list);
        return tag;
    }

    public static SceneSavedData load(CompoundTag tag) {
        SceneSavedData data = new SceneSavedData();
        ListTag list = tag.getList("Scenes", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                data.scenes.add(Scene.fromNbt(list.getCompound(i)));
            } catch (Exception ignored) {
                // Skip corrupt entries rather than failing the whole load.
            }
        }
        return data;
    }

    public static SceneSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SceneSavedData::load, SceneSavedData::new, DATA_NAME);
    }
}
