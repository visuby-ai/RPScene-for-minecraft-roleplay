package com.rpscene.client;

import com.rpscene.FloatingMessageChannel;

/**
 * A single active floating text line being rendered above an entity's
 * head - from {@code /me}, {@code /do}, or {@code /ooc}. Ephemeral and
 * client-only; never persisted.
 */
public final class FloatingMessage {

    private final String text;
    private final FloatingMessageChannel channel;
    private final long createdAtMillis;
    private final long durationMillis;

    public FloatingMessage(String text, FloatingMessageChannel channel, int durationSeconds) {
        this.text = text;
        this.channel = channel;
        this.createdAtMillis = System.currentTimeMillis();
        this.durationMillis = durationSeconds * 1000L;
    }

    public String getText() {
        return text;
    }

    public FloatingMessageChannel getChannel() {
        return channel;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAtMillis >= durationMillis;
    }

    /** 1.0 = fully opaque, 0.0 = fully faded, ramping down over the last 25% of its lifetime. */
    public float getAlpha() {
        long elapsed = System.currentTimeMillis() - createdAtMillis;
        long fadeStart = (long) (durationMillis * 0.75);
        if (elapsed <= fadeStart) {
            return 1.0f;
        }
        long fadeWindow = durationMillis - fadeStart;
        if (fadeWindow <= 0) {
            return 0.0f;
        }
        float progress = (float) (elapsed - fadeStart) / fadeWindow;
        return Math.max(0.0f, 1.0f - progress);
    }
}
