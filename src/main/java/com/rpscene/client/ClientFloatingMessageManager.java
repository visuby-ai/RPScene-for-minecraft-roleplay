package com.rpscene.client;

import com.rpscene.FloatingMessageChannel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active floating messages per entity as a single unified
 * vertical stack shared across all channels ({@code /me}, {@code /do},
 * {@code /ooc}) - each new message appears in the slot closest to the
 * entity's head, and existing messages shift up one slot to make room.
 * Because all channels share one stack, messages of different kinds can
 * never visually overlap. Each message still fades and expires
 * independently based on its own duration.
 */
public final class ClientFloatingMessageManager {

    private static final ClientFloatingMessageManager INSTANCE = new ClientFloatingMessageManager();

    /** Caps how many lines can stack at once so spam can't grow this unbounded. */
    private static final int MAX_STACK_PER_ENTITY = 8;

    private final Map<Integer, Deque<FloatingMessage>> messagesByEntity = new ConcurrentHashMap<>();

    private ClientFloatingMessageManager() {
    }

    public static ClientFloatingMessageManager get() {
        return INSTANCE;
    }

    /**
     * Adds a new message to the front (bottom-most/nearest-head slot) of
     * the entity's stack. If the stack is already at capacity, the
     * oldest (top-most) message is dropped to make room.
     */
    public void addMessage(int entityId, String text, FloatingMessageChannel channel, int durationSeconds) {
        Deque<FloatingMessage> stack = messagesByEntity.computeIfAbsent(entityId, k -> new ArrayDeque<>());
        synchronized (stack) {
            stack.addFirst(new FloatingMessage(text, channel, durationSeconds));
            while (stack.size() > MAX_STACK_PER_ENTITY) {
                stack.removeLast();
            }
        }
    }

    /**
     * Returns the currently active (non-expired) messages for an entity,
     * newest first. Index 0 is the slot nearest the entity's head; each
     * following index should be rendered one line further up.
     */
    public List<FloatingMessage> getActive(int entityId) {
        Deque<FloatingMessage> stack = messagesByEntity.get(entityId);
        if (stack == null) {
            return List.of();
        }
        synchronized (stack) {
            stack.removeIf(FloatingMessage::isExpired);
            return new ArrayList<>(stack);
        }
    }

    /** Immediately removes every active message of the given channel for an entity (used by /ooc clear). */
    public void clearChannel(int entityId, FloatingMessageChannel channel) {
        Deque<FloatingMessage> stack = messagesByEntity.get(entityId);
        if (stack == null) {
            return;
        }
        synchronized (stack) {
            stack.removeIf(message -> message.getChannel() == channel);
        }
    }

    public void clear() {
        messagesByEntity.clear();
    }
}
