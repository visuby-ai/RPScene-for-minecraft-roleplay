package com.rpscene.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.rpscene.Config;
import com.rpscene.scene.DurationParser;
import com.rpscene.scene.Scene;
import com.rpscene.scene.SceneManager;
import com.rpscene.scene.SceneType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /scene create|list|remove|tp} - persistent world-anchored scene
 * marker management. {@code create} and {@code list} are usable by
 * anyone; {@code remove} requires ownership or op; {@code tp} is
 * admin-only.
 */
public final class SceneCommand {

    private static final int DEFAULT_LIST_RADIUS = 48;
    private static final int OP_PERMISSION_LEVEL = 2;

    private static final List<String> DURATION_EXAMPLES =
            Arrays.asList("10s", "30s", "1m", "5m", "30m", "1h", "12h", "1d", "7d");

    private SceneCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scene")
                .then(Commands.literal("create")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .suggests(SceneCommand::suggestCreateArgs)
                                .executes(SceneCommand::executeCreate)))
                .then(Commands.literal("list")
                        .executes(SceneCommand::executeList))
                .then(Commands.literal("remove")
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .suggests(SceneCommand::suggestSceneIds)
                                .executes(SceneCommand::executeRemove)))
                .then(Commands.literal("tp")
                        .requires(source -> source.hasPermission(OP_PERMISSION_LEVEL))
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .suggests(SceneCommand::suggestSceneIds)
                                .executes(SceneCommand::executeTeleport))));
    }

    /**
     * {@code /scene create [type] [duration] <message>} - creates a
     * scene marker at the sender's position.
     * <p>
     * Supported forms:
     * <pre>
     * /scene create &lt;message&gt;
     * /scene create &lt;duration&gt; &lt;message&gt;
     * /scene create &lt;type&gt; &lt;duration&gt; &lt;message&gt;
     * </pre>
     * The leading tokens are parsed greedily: the first token is checked
     * against known scene types, the next (or first, if no type
     * matched) against the duration grammar, and everything remaining
     * becomes the scene text.
     */
    private static int executeCreate(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use /scene create."));
            return 0;
        }

        String raw = StringArgumentType.getString(ctx, "message").trim();
        if (raw.isEmpty()) {
            source.sendFailure(Component.translatable("commands.rpscene.do.usage"));
            return 0;
        }

        String[] tokens = raw.split(" ", 3);

        SceneType type = SceneType.SCENE;
        long durationMillis = -1;
        String message;

        int consumed = 0;
        if (tokens.length > 0 && SceneType.isTypeToken(tokens[0])) {
            type = SceneType.fromString(tokens[0]);
            consumed = 1;
        }

        if (consumed < tokens.length) {
            Optional<Long> maybeDuration = DurationParser.parseMillis(tokens[consumed]);
            if (maybeDuration.isPresent()) {
                durationMillis = maybeDuration.get();
                consumed++;
            }
        }

        // Re-split the remainder out of the original string so message text
        // itself may safely contain spaces without being clipped by the
        // earlier 3-way split.
        message = rebuildMessage(raw, consumed);

        if (message.isEmpty()) {
            source.sendFailure(Component.translatable("commands.rpscene.do.usage"));
            return 0;
        }

        SceneManager.get().create(
                player.getUUID(),
                player.getGameProfile().getName(),
                message,
                type,
                player.blockPosition(),
                player.level().dimension().location(),
                durationMillis,
                source.getServer()
        );

        SceneType finalType = type;
        String finalMessage = message;
        source.sendSuccess(() -> Component.literal(finalType.getIcon() + " " + finalMessage), false);
        return 1;
    }

    /**
     * Drops the first {@code tokensToDrop} whitespace-delimited tokens from
     * {@code raw} and returns the remainder, preserving internal spacing of
     * the message itself.
     */
    private static String rebuildMessage(String raw, int tokensToDrop) {
        String remainder = raw;
        for (int i = 0; i < tokensToDrop; i++) {
            int spaceIndex = remainder.indexOf(' ');
            if (spaceIndex < 0) {
                return "";
            }
            remainder = remainder.substring(spaceIndex + 1);
        }
        return remainder.trim();
    }

    /**
     * Offers tab-complete hints for the first one or two tokens of
     * /scene create: scene type keywords and duration examples on the
     * first token, and duration examples on the second token if the
     * first was a recognized type.
     */
    private static CompletableFuture<Suggestions> suggestCreateArgs(CommandContext<CommandSourceStack> ctx,
                                                                      SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        String[] tokens = remaining.split(" ", -1);
        int tokenIndex = tokens.length - 1;
        String currentToken = tokens[tokenIndex];

        List<String> suggestions = new ArrayList<>();
        if (tokenIndex == 0) {
            for (SceneType type : SceneType.values()) {
                suggestions.add(type.getId());
            }
            suggestions.addAll(DURATION_EXAMPLES);
        } else if (tokenIndex == 1 && SceneType.isTypeToken(tokens[0])) {
            suggestions.addAll(DURATION_EXAMPLES);
        }

        if (suggestions.isEmpty()) {
            return builder.buildFuture();
        }

        SuggestionsBuilder offsetBuilder =
                builder.createOffset(builder.getStart() + remaining.length() - currentToken.length());
        return SharedSuggestionProvider.suggest(suggestions, offsetBuilder);
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can list nearby scenes."));
            return 0;
        }

        List<Scene> nearby = SceneManager.get().getNear(player.level(), player.blockPosition(), DEFAULT_LIST_RADIUS);
        if (nearby.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.rpscene.scene.list_empty"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("commands.rpscene.scene.list_header", nearby.size()), false);
        for (Scene scene : nearby) {
            String remaining = scene.isPersistent() ? "permanent"
                    : DurationParser.formatRemaining(scene.getRemainingMillis(System.currentTimeMillis()));
            String line = String.format("%s [%s] %s - %s (by %s, %s)",
                    scene.getType().getIcon(), shortId(scene.getId()), scene.getText(),
                    remaining, scene.getOwnerName(), scene.getType().getId());
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return nearby.size();
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        UUID sceneId = resolveSceneId(source, StringArgumentType.getString(ctx, "uuid"));
        if (sceneId == null) {
            source.sendFailure(Component.translatable("commands.rpscene.scene.not_found"));
            return 0;
        }

        Scene scene = SceneManager.get().getById(sceneId);
        if (scene == null) {
            source.sendFailure(Component.translatable("commands.rpscene.scene.not_found"));
            return 0;
        }

        boolean isOwner = source.getEntity() instanceof ServerPlayer player && player.getUUID().equals(scene.getOwnerId());
        boolean isOp = source.hasPermission(OP_PERMISSION_LEVEL) && Config.ALLOW_NON_OWNER_REMOVE_FOR_OPS.get();
        if (!isOwner && !isOp) {
            source.sendFailure(Component.translatable("commands.rpscene.scene.no_permission"));
            return 0;
        }

        SceneManager.get().remove(sceneId, source.getServer());
        source.sendSuccess(() -> Component.translatable("commands.rpscene.scene.removed"), false);
        return 1;
    }

    private static int executeTeleport(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        UUID sceneId = resolveSceneId(source, StringArgumentType.getString(ctx, "uuid"));
        if (sceneId == null) {
            source.sendFailure(Component.translatable("commands.rpscene.scene.not_found"));
            return 0;
        }

        Scene scene = SceneManager.get().getById(sceneId);
        if (scene == null) {
            source.sendFailure(Component.translatable("commands.rpscene.scene.not_found"));
            return 0;
        }

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can teleport to a scene."));
            return 0;
        }

        ServerLevel targetLevel = source.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION, scene.getDimension()));
        if (targetLevel == null) {
            source.sendFailure(Component.literal("Scene's dimension is not currently loaded."));
            return 0;
        }

        BlockPos pos = scene.getPos();
        player.teleportTo(targetLevel, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        return 1;
    }

    /**
     * Scenes are addressed by short id in {@code /scene list} output for
     * readability, so this resolves either a full UUID or a unique short
     * prefix match against currently known scenes.
     */
    private static UUID resolveSceneId(CommandSourceStack source, String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {
            // Fall through to short-id resolution.
        }

        Scene match = null;
        for (Scene scene : SceneManager.get().getAll()) {
            if (shortId(scene.getId()).equalsIgnoreCase(input)) {
                if (match != null) {
                    return null; // ambiguous
                }
                match = scene;
            }
        }
        return match != null ? match.getId() : null;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    /** Offers every currently known scene's short id as a tab-complete suggestion. */
    private static CompletableFuture<Suggestions> suggestSceneIds(CommandContext<CommandSourceStack> ctx,
                                                                    SuggestionsBuilder builder) {
        List<String> ids = new ArrayList<>();
        for (Scene scene : SceneManager.get().getAll()) {
            ids.add(shortId(scene.getId()));
        }
        return SharedSuggestionProvider.suggest(ids, builder);
    }
}
