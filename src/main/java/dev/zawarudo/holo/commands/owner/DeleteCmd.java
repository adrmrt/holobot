package dev.zawarudo.holo.commands.owner;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@CommandInfo(name = "delete",
    description = "Deletes a message of your choice. This works by either passing the message id or replying to a message.",
    usage = "[msg id]",
    alias = {"d"},
    ownerOnly = true,
    category = CommandCategory.OWNER)
public class DeleteCmd extends AbstractCommand implements ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        Optional<Message> referenced = ctx.message().map(Message::getReferencedMessage);
        if (referenced.isPresent()) {
            referenced.get().delete().queue();
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();

        if (ctx.argCount() != 1) {
            builder.setTitle("Incorrect Usage");
            builder.setDescription("Please only provide the id of the message you want to delete!");
            ctx.notifyOwner(builder);
            return;
        }

        long id;
        try {
            id = Long.parseLong(ctx.args().getFirst());
        } catch (NumberFormatException ex) {
            builder.setTitle("Error");
            builder.setDescription("Please provide the id of the message you want to delete!");
            ctx.notifyOwner(builder);
            return;
        }

        ctx.channel().retrieveMessageById(id).complete().delete().queue(v -> {
        }, _ -> {
        });
    }
}
