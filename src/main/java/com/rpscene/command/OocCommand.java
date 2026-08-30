package com.rpscene.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.rpscene.Config;
import com.rpscene.FloatingMessageChannel;
import com.rpscene.network.NetworkHandler;
import com.rpscene.network.packet.FloatingMessageClearPacket;
import com.rpscene.network.packet.FloatingMessagePacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /ooc [duration] <message>} - an out-of-character aside shown
 * above the sender's head like {@code /me}/{@code /do}, but in its own
 * color and wrapped in {@code (( ))} per the usual roleplay convention
 * for OOC text. Shares the same per-entity message stack as
 * {@code /me}/{@code /do} so nothing overlaps.
 * <p>
 * {@code /ooc remove} clears the sender's own currently active OOC
 * message(s) early, ahead of their natural expiration.
 */
public final class OocCommand {

    private static final List<String> DURATION_EXAMPLES =
            Arrays.asList("5s", "10s", "15s", "30s", "1m", "2m", "5m");

    private OocCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ooc")
                .then(Commands.literal("remove")
                        .executes(OocCommand::executeRemove))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .suggests(OocCommand::suggestDuration)
                        .executes(OocCommand::executeSay)));
    }

    private static int executeSay(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use /ooc."));
            return 0;
        }

        String raw = StringArgumentType.getString(ctx, "message").trim();
        if (raw.isEmpty()) {
            return 0;
        }

        // Optional leading duration token, e.g. "/ooc 30s brb" - falls back
        // to the configured default when the first token isn't a duration.
        int durationSeconds = Config.OOC_DURATION.get();
        String message = raw;

        int firstSpace = raw.indexOf(' ');
        String firstToken = firstSpace < 0 ? raw : raw.substring(0, firstSpace);
        Optional<Long> maybeDuration = com.rpscene.scene.DurationParser.parseMillis(firstToken);
        if (maybeDuration.isPresent() && firstSpace >= 0) {
            durationSeconds = (int) Math.max(1, maybeDuration.get() / 1000L);
            message = raw.substring(firstSpace + 1).trim();
        }

        if (message.isEmpty()) {
            return 0;
        }

        double range = Config.OOC_RANGE.get();
        int finalDuration = durationSeconds;

        FloatingMessagePacket packet = new FloatingMessagePacket(
                player.getId(), FloatingMessageChannel.OOC, message, finalDuration);
        NetworkHandler.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        player.getX(), player.getY(), player.getZ(), range, player.level().dimension())),
                packet
        );

        return 1;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use /ooc remove."));
            return 0;
        }

        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new FloatingMessageClearPacket(player.getId(), FloatingMessageChannel.OOC));
        source.sendSuccess(() -> Component.translatable("commands.rpscene.ooc.removed"), false);
        return 1;
    }

    /** Suggests duration examples only while typing the first token. */
    private static CompletableFuture<Suggestions> suggestDuration(CommandContext<CommandSourceStack> ctx,
                                                                    SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        if (remaining.contains(" ")) {
            return builder.buildFuture();
        }
        return SharedSuggestionProvider.suggest(DURATION_EXAMPLES, builder);
    }
}
