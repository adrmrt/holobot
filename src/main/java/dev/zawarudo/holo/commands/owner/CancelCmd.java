package dev.zawarudo.holo.commands.owner;

import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * Command to cancel all requests to JDA.
 */
@CommandInfo(name = "cancel",
    description = "Cancels all the ongoing requests.",
    ownerOnly = true,
    category = CommandCategory.OWNER)
public class CancelCmd implements CommandMetadata, ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();
        ctx.jda().cancelRequests();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Success");
        builder.setDescription("Cancelled all requests");
        builder.setTimestamp(Instant.now());
        ctx.notifyOwner(builder);
    }
}
