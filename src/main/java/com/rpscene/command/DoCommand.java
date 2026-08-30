package com.rpscene.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.rpscene.Config;
import com.rpscene.FloatingMessageChannel;
import com.rpscene.network.NetworkHandler;
import com.rpscene.network.packet.FloatingMessagePacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * {@code /do <prompt>} - a stylistically distinct companion to
 * {@code /me}, meant for GM-style follow-up cues that build on an
 * action rather than describing one, e.g.:
 * <pre>
 * /me picks up a zip tie and binds the man's hands
 * /do resist or not?
 * </pre>
 * Renders as floating text above the sender's head just like
 * {@code /me}, sharing the same per-entity message stack (so the two
 * never visually overlap), but in a different color with a distinct
 * prefix so it reads as a separate "channel" from in-character action
 * text.
 * <p>
 * Unlike {@code /me}, vanilla Minecraft has no built-in {@code /do}
 * command, so there is no Brigadier tree-merge conflict to work around
 * here - a plain greedy string argument is fine.
 * <p>
 * The old ground-marker behavior previously on {@code /do} now lives at
 * {@code /scene create [type] [duration] <message>}.
 */
public final class DoCommand {

    private DoCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("do")
                .then(Commands.argument("prompt", StringArgumentType.greedyString())
                        .executes(DoCommand::execute)));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use /do."));
            return 0;
        }

        String prompt = StringArgumentType.getString(ctx, "prompt").trim();
        if (prompt.isEmpty()) {
            return 0;
        }

        int duration = Config.ME_DURATION.get();
        double range = Config.ME_RANGE.get();

        FloatingMessagePacket packet = new FloatingMessagePacket(player.getId(), FloatingMessageChannel.DO, prompt, duration);
        NetworkHandler.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        player.getX(), player.getY(), player.getZ(), range, player.level().dimension())),
                packet
        );

        return 1;
    }
}
