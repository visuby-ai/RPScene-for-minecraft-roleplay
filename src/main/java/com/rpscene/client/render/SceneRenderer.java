package com.rpscene.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rpscene.Config;
import com.rpscene.RPSceneMod;
import com.rpscene.client.ClientSceneManager;
import com.rpscene.scene.Scene;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Renders each nearby scene as a small billboarded icon + one-line label
 * that always faces the camera, fades with distance, and stays minimal
 * (per spec: "avoid large floating UI panels").
 */
@Mod.EventBusSubscriber(modid = RPSceneMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SceneRenderer {

    private static final float TEXT_SCALE = 0.025f;

    private SceneRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        double renderRange = Config.SCENE_RENDER_RANGE.get();
        double fadeStartFraction = Config.SCENE_FADE_START.get();

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        BlockPos playerPos = mc.player.blockPosition();

        List<Scene> scenes = ClientSceneManager.get().getInDimension(mc.level.dimension().location());
        if (scenes.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        double renderRangeSq = renderRange * renderRange;

        for (Scene scene : scenes) {
            BlockPos pos = scene.getPos();
            double distSq = pos.distSqr(playerPos);
            if (distSq > renderRangeSq) {
                continue;
            }

            double dist = Math.sqrt(distSq);
            float alpha = computeFadeAlpha(dist, renderRange, fadeStartFraction);
            if (alpha <= 0.01f) {
                continue;
            }

            String label = scene.getType().getIcon() + " " + trimForDisplay(scene.getText());
            renderBillboardLabel(poseStack, bufferSource, font, label,
                    pos.getX() + 0.5 - camPos.x,
                    pos.getY() + 1.35 - camPos.y,
                    pos.getZ() + 0.5 - camPos.z,
                    camera, alpha);
        }

        bufferSource.endBatch();
    }

    private static float computeFadeAlpha(double dist, double renderRange, double fadeStartFraction) {
        double fadeStart = renderRange * fadeStartFraction;
        if (dist <= fadeStart) {
            return 1.0f;
        }
        if (dist >= renderRange) {
            return 0.0f;
        }
        return (float) (1.0 - (dist - fadeStart) / (renderRange - fadeStart));
    }

    private static String trimForDisplay(String text) {
        final int maxChars = 40;
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars - 1) + "\u2026";
    }

    private static void renderBillboardLabel(PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                                              String text, double relX, double relY, double relZ,
                                              Camera camera, float alpha) {
        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        int textWidth = font.width(text);
        int backgroundColor = (int) (alpha * 80) << 24;
        int textColor = 0xFFFFFF | ((int) (alpha * 255) << 24);

        font.drawInBatch(text, -textWidth / 2f, 0, textColor, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH,
                backgroundColor, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }
}
