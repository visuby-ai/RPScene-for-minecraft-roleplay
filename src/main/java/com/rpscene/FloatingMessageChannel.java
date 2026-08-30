package com.rpscene;

/**
 * The three kinds of floating text a player can put above their own
 * head: {@code /me} actions, {@code /do} prompts (a stylistically
 * distinct companion to /me, meant for GM-style follow-up cues), and
 * {@code /ooc} out-of-character asides.
 * <p>
 * All three share one rendering stack per entity (see
 * {@code ClientFloatingMessageManager}) so messages from different
 * channels never visually overlap - only their prefix/suffix and color
 * differ.
 */
public enum FloatingMessageChannel {

    ME((byte) 0, "* ", "", 0xFFFFFF, 0x000000),
    DO((byte) 1, ">> ", "", 0xFFC96B, 0x2E2000),
    OOC((byte) 2, "(( ", " ))", 0x8FD9FF, 0x00202A);

    private final byte id;
    private final String prefix;
    private final String suffix;
    private final int textColor;
    private final int backgroundColor;

    FloatingMessageChannel(byte id, String prefix, String suffix, int textColor, int backgroundColor) {
        this.id = id;
        this.prefix = prefix;
        this.suffix = suffix;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
    }

    public byte getId() {
        return id;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    /** Base RGB (no alpha) text color for this channel. */
    public int getTextColor() {
        return textColor;
    }

    /** Base RGB (no alpha) background tint for this channel. */
    public int getBackgroundColor() {
        return backgroundColor;
    }

    public static FloatingMessageChannel byId(byte id) {
        for (FloatingMessageChannel channel : values()) {
            if (channel.id == id) {
                return channel;
            }
        }
        return ME;
    }
}
