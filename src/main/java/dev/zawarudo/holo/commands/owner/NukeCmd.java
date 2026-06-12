package dev.zawarudo.holo.commands.owner;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandInfo(name = "nuke",
    description = "Deletes a given amount of messages indiscriminately within the channel.",
    usage = "<amount>",
    ownerOnly = true,
    category = CommandCategory.OWNER)
public class NukeCmd implements CommandMetadata, ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        if (!(ctx.channel() instanceof TextChannel tc)) {
            return;
        }

        if (!ctx.hasArgs()) {
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(ctx.args().getFirst());
            if (amount < 2) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            return;
        }

        int remaining = amount;
        while (remaining > 0) {
            int batch = Math.min(remaining, 100);
            deleteMessagesFromChannel(tc, batch);
            remaining -= batch;
        }
    }

    /**
     * Deletes a given amount of messages from a channel.
     */
    private void deleteMessagesFromChannel(TextChannel channel, int amount) {
        List<Message> messages = channel.getHistory().retrievePast(amount).complete();
        channel.deleteMessages(messages).queue();
    }
}
