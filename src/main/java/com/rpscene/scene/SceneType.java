package com.rpscene.scene;

import java.util.Locale;

/**
 * Supported scene categories, each with a display icon used in the world
 * marker, chat feedback, and the inspection UI.
 */
public enum SceneType {
    SCENE("scene", "\uD83D\uDCCC"),       // 📌
    BLOOD("blood", "\uD83E\uDE78"),       // 🩸
    FOOTPRINT("footprint", "\uD83D\uDC63"), // 👣
    WEAPON("weapon", "\uD83D\uDD2B"),     // 🔫
    NOTE("note", "\uD83D\uDCDD"),         // 📝
    WARNING("warning", "\u26A0"),         // ⚠
    EVIDENCE("evidence", "\uD83D\uDD0D"); // 🔍

    public static final String FALLBACK_ICON = "\uD83D\uDCCC"; // 📌

    private final String id;
    private final String icon;

    SceneType(String id, String icon) {
        this.id = id;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public String getIcon() {
        return icon;
    }

    /**
     * Resolves a scene type from a command argument. Unknown values fall
     * back to {@link #SCENE} so the icon is never missing.
     */
    public static SceneType fromString(String value) {
        if (value == null) {
            return SCENE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (SceneType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        return SCENE;
    }

    /**
     * Whether the given token looks like a valid scene type keyword at all
     * (used to distinguish `/do <type> <duration> <msg>` from `/do <msg>`).
     */
    public static boolean isTypeToken(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (SceneType type : values()) {
            if (type.id.equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
