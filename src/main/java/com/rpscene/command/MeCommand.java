package com.rpscene.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rpscene.Config;
import com.rpscene.FloatingMessageChannel;
import com.rpscene.network.NetworkHandler;
import com.rpscene.network.packet.FloatingMessagePacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * {@code /me <action>} - broadcasts a floating roleplay action above the
 * sender's head to nearby players, replacing vanilla's built-in chat-based
 * {@code /me}.
 * <p>
 * Vanilla already registers a {@code /me <action>} command whose argument
 * node is named "action" and typed as {@link MessageArgument}. Brigadier
 * merges any command registration that reuses the same literal/argument
 * <em>names</em> into the existing tree, regardless of argument type -
 * the original node (and its type) is kept, and only the executor
 * ("command") is replaced by whichever registration ran most recently.
 * <p>
 * If our node used a different argument type (e.g. a plain greedy
 * string) under the same name "action", Brigadier would still parse
 * using vanilla's original {@link MessageArgument} node, but then our
 * executor would try to read the result back out as a {@code String},
 * causing a runtime {@code IllegalArgumentException} ("Argument 'action'
 * is defined as Message, not class java.lang.String"). Matching
 * vanilla's exact argument name and type here avoids that entirely, and
 * cleanly overrides vanilla's chat broadcast with our floating text
 * instead.
 */
public final class MeCommand {

    private MeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("me")
                .then(Commands.argument("action", MessageArgument.message())
                        .executes(MeCommand::execute)));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use /me."));
            return 0;
        }

        // Resolves entity selectors (e.g. "/me hugs @p") into their names
        // and returns the final display text as a Component, matching
        // vanilla's own resolution behavior for this argument.
        Component actionComponent = MessageArgument.getMessage(ctx, "action");
        String action = actionComponent.getString().trim();
        if (action.isEmpty()) {
            return 0;
        }

        int duration = Config.ME_DURATION.get();
        double range = Config.ME_RANGE.get();

        FloatingMessagePacket packet = new FloatingMessagePacket(player.getId(), FloatingMessageChannel.ME, action, duration);
        NetworkHandler.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        player.getX(), player.getY(), player.getZ(), range, player.level().dimension())),
                packet
        );

        return 1;
    }
}
