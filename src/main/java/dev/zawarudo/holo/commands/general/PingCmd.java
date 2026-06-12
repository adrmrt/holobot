package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@CommandInfo(name = "ping",
    description = "Shows the ping of the bot",
    alias = {"pong"},
    category = CommandCategory.GENERAL)
public class PingCmd implements CommandMetadata, ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Pong!");
        builder.setDescription("Ping: `...` ms\nHeartbeat: `...` ms");
        ctx.member().ifPresent(m ->
            builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl())
        );
        long start = System.currentTimeMillis();
        Message message = ctx.channel().sendMessageEmbeds(builder.build()).complete();
        long ms = System.currentTimeMillis() - start;
        builder.setDescription("Ping: `" + ms + "` ms\nHeartbeat: `" + ctx.jda().getGatewayPing() + "` ms");
        message.editMessageEmbeds(builder.build()).queue();
        message.delete().queueAfter(1, TimeUnit.MINUTES);
    }
}
