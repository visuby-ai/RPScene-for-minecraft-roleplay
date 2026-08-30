package com.rpscene.client.screen;

import com.rpscene.scene.DurationParser;
import com.rpscene.scene.Scene;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Minimal, immersive scene inspection panel - deliberately small and
 * text-first (no large UI chrome), matching the spec's "minimal UI style"
 * requirement.
 */
public class SceneDetailsScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int LINE_HEIGHT = 12;

    private final Scene scene;

    public SceneDetailsScreen(Scene scene) {
        super(Component.translatable("gui.rpscene.details.title"));
        this.scene = scene;
    }

    @Override
    protected void init() {
        super.init();
        int panelLeft = (width - PANEL_WIDTH) / 2;
        int buttonY = panelTop() + panelHeight() - 24;

        addRenderableWidget(Button.builder(Component.translatable("gui.rpscene.details.close"),
                        button -> onClose())
                .bounds(panelLeft + PANEL_WIDTH - 70, buttonY, 60, 20)
                .build());
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }

    private int panelHeight() {
        return 110;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int panelLeft = (width - PANEL_WIDTH) / 2;
        int panelTop = panelTop();
        int panelHeight = panelHeight();

        graphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + panelHeight, 0xC0101010);
        graphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + 1, 0xFF444444);
        graphics.fill(panelLeft, panelTop + panelHeight - 1, panelLeft + PANEL_WIDTH, panelTop + panelHeight, 0xFF444444);

        int textX = panelLeft + 10;
        int y = panelTop + 8;

        graphics.drawString(font, title, textX, y, 0xFFAAAAAA, false);
        y += LINE_HEIGHT + 2;

        String headline = scene.getType().getIcon() + " " + scene.getText();
        graphics.drawWordWrap(font, Component.literal(headline), textX, y, PANEL_WIDTH - 20, 0xFFFFFFFF);
        y += LINE_HEIGHT * wrappedLineCount(headline) + 4;

        graphics.drawString(font, Component.translatable("gui.rpscene.details.created_by"), textX, y, 0xFF888888, false);
        graphics.drawString(font, scene.getOwnerName(), textX + 90, y, 0xFFFFFFFF, false);
        y += LINE_HEIGHT;

        graphics.drawString(font, Component.translatable("gui.rpscene.details.type"), textX, y, 0xFF888888, false);
        graphics.drawString(font, scene.getType().getId(), textX + 90, y, 0xFFFFFFFF, false);
        y += LINE_HEIGHT;

        graphics.drawString(font, Component.translatable("gui.rpscene.details.created"), textX, y, 0xFF888888, false);
        graphics.drawString(font, formatTime(scene.getCreationTime()), textX + 90, y, 0xFFFFFFFF, false);
        y += LINE_HEIGHT;

        graphics.drawString(font, Component.translatable("gui.rpscene.details.remaining"), textX, y, 0xFF888888, false);
        String remaining = scene.isPersistent()
                ? Component.translatable("gui.rpscene.details.permanent").getString()
                : DurationParser.formatRemaining(scene.getRemainingMillis(System.currentTimeMillis()));
        graphics.drawString(font, remaining, textX + 90, y, 0xFFFFFFFF, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int wrappedLineCount(String text) {
        int width = PANEL_WIDTH - 20;
        int textWidth = font.width(text);
        return Math.max(1, (textWidth / width) + 1);
    }

    private static String formatTime(long epochMillis) {
        return new SimpleDateFormat("HH:mm").format(new Date(epochMillis));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
