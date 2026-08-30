package com.rpscene;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Common configuration for RP Scene.
 * <p>
 * Mirrors the JSON shape from the spec:
 * <pre>
 * {
 *   "me_duration": 10,
 *   "me_range": 32
 * }
 * </pre>
 */
public final class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue ME_DURATION = BUILDER
            .comment("Seconds a /me floating action stays visible before fading out.",
                    "Also used by /do, which is styled differently but shares /me's timing.")
            .defineInRange("me_duration", 10, 1, 120);

    public static final ForgeConfigSpec.IntValue ME_RANGE = BUILDER
            .comment("Block radius within which players can see a /me or /do floating action.")
            .defineInRange("me_range", 32, 4, 128);

    public static final ForgeConfigSpec.IntValue OOC_DURATION = BUILDER
            .comment("Default seconds an /ooc message stays visible before fading out,",
                    "used when no explicit duration is given to /ooc.")
            .defineInRange("ooc_duration", 15, 1, 300);

    public static final ForgeConfigSpec.IntValue OOC_RANGE = BUILDER
            .comment("Block radius within which players can see an /ooc message.")
            .defineInRange("ooc_range", 32, 4, 128);

    public static final ForgeConfigSpec.IntValue SCENE_RENDER_RANGE = BUILDER
            .comment("Block radius within which scene markers are rendered client-side.")
            .defineInRange("scene_render_range", 48, 8, 256);

    public static final ForgeConfigSpec.DoubleValue SCENE_FADE_START = BUILDER
            .comment("Fraction of scene_render_range at which distance fade begins (0.0-1.0).")
            .defineInRange("scene_fade_start", 0.7, 0.0, 0.99);

    public static final ForgeConfigSpec.IntValue SCENE_INSPECT_RANGE = BUILDER
            .comment("Max distance in blocks a player can be from a scene to inspect it with the F key.")
            .defineInRange("scene_inspect_range", 6, 1, 32);

    public static final ForgeConfigSpec.BooleanValue ALLOW_NON_OWNER_REMOVE_FOR_OPS = BUILDER
            .comment("If true, server operators can always remove any scene regardless of owner.")
            .define("allow_op_remove", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}
