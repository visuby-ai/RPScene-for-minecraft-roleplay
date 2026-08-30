package com.rpscene.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rpscene.FloatingMessageChannel;
import com.rpscene.RPSceneMod;
import com.rpscene.client.ClientFloatingMessageManager;
import com.rpscene.client.FloatingMessage;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Renders active floating messages ({@code /me}, {@code /do},
 * {@code /ooc}) above player heads, per the spec: rendered as floating
 * world text, never in chat.
 * <p>
 * All three channels share one per-entity vertical stack: the newest
 * message sits in the slot closest to the head, and each older one is
 * drawn one line further up, so mixing /me, /do, and /ooc in sequence
 * reads top-to-bottom as history rather than overlapping. Each
 * channel's {@link FloatingMessageChannel} carries its own prefix/suffix
 * and color so the three read as visually distinct "channels" even
 * while sharing the same stack.
 */
@Mod.EventBusSubscriber(modid = RPSceneMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FloatingMessageRenderer {

    private static final float TEXT_SCALE = 0.025f;
    private static final double VERTICAL_OFFSET = 0.6;
    private static final double LINE_SPACING = 0.28;

    private FloatingMessageRenderer() {
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

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Player)) {
                continue;
            }
            List<FloatingMessage> messages = ClientFloatingMessageManager.get().getActive(entity.getId());
            if (messages.isEmpty()) {
                continue;
            }

            double baseX = entity.getX() - camPos.x;
            double baseY = entity.getY() + entity.getBbHeight() + VERTICAL_OFFSET - camPos.y;
            double baseZ = entity.getZ() - camPos.z;

            for (int i = 0; i < messages.size(); i++) {
                FloatingMessage message = messages.get(i);
                float alpha = message.getAlpha();
                if (alpha <= 0.01f) {
                    continue;
                }

                FloatingMessageChannel channel = message.getChannel();
                String text = channel.getPrefix() + message.getText() + channel.getSuffix();

                double y = baseY + i * LINE_SPACING;
                renderBillboardLabel(poseStack, bufferSource, font, text,
                        baseX, y, baseZ, camera, alpha, channel.getTextColor(), channel.getBackgroundColor());
            }
        }

        bufferSource.endBatch();
    }

    private static void renderBillboardLabel(PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                                              String text, double relX, double relY, double relZ,
                                              Camera camera, float alpha, int textRgb, int backgroundRgb) {
        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        int textWidth = font.width(text);
        int backgroundColor = ((int) (alpha * 80) << 24) | (backgroundRgb & 0xFFFFFF);
        int textColor = (textRgb & 0xFFFFFF) | ((int) (alpha * 255) << 24);

        font.drawInBatch(text, -textWidth / 2f, 0, textColor, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH,
                backgroundColor, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }
}
