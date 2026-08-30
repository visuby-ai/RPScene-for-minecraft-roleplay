package com.rpscene.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;

/** Client key bindings for RP Scene. */
public final class KeyBindings {

    public static final String CATEGORY = "key.categories.rpscene";

    public static final KeyMapping INSPECT_SCENE = new KeyMapping(
            "key.rpscene.inspect",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F,
            CATEGORY
    );

    private KeyBindings() {
    }
}
