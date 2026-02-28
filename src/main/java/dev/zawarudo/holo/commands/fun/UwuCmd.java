package dev.zawarudo.holo.commands.fun;

import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

@CommandInfo(name = "uwu",
        description = "Uwuify a text of your choice. You can also reply to a message to uwuify it.",
        usage = "<text>",
        category = CommandCategory.MISC)
public class UwuCmd extends AbstractCommand implements ExecutableCommand {

    private static final Random rand = new Random();

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        final List<String> args = ctx.args();
        final Message invokingMessage = ctx.message().orElse(null);
        final Message referenced = invokingMessage != null
                ? invokingMessage.getReferencedMessage()
                : null;

        if (args.isEmpty() && referenced == null) {
            ctx.reply().errorEmbed("Please provide text or reply to a message!");
            return;
        }

        // Choose which message to uwuify
        Message msg = referenced != null ? referenced : invokingMessage;
        if (msg == null) {
            // Should never happen
            ctx.reply().errorEmbed("Please provide text or reply to a message!");
            return;
        }

        // Disallow pings
        if (!msg.getMentions().getMembers().isEmpty()
                || !msg.getMentions().getRoles().isEmpty()
                || !msg.getMentions().getUsers().isEmpty()) {
            ctx.reply().errorEmbed("No pings!");
            return;
        }

        // No custom emojis
        if (!msg.getMentions().getCustomEmojis().isEmpty()) {
            ctx.reply().errorEmbed("I can't use custom emojis!");
            return;
        }

        final String input;
        if (referenced != null) {
            input = referenced.getContentRaw();
        } else {
            input = String.join(" ", args);
        }

        final String uwu = uwuify(input);
        sendText(ctx, uwu);
    }

    private void sendText(CommandContext ctx, String text) {
        int index = 0;
        while (index < text.length()) {
            String chunk = text.substring(
                    index,
                    Math.min(index + Message.MAX_CONTENT_LENGTH, text.length())
            );
            ctx.reply().text(chunk);
            index += Message.MAX_CONTENT_LENGTH;
        }
    }

    private static String uwuify(String stringToUwuify) {
        String result = stringToUwuify
                .toLowerCase()
                .replaceAll("[rl]", "w")
                .replaceAll("n([aeiou])", "ny$1")
                .replace("ove", "uve")
                .replace("uck", "uwq")
                .replaceFirst("i", "i-i")
                .replaceFirst("(?s)(.*)" + "i-i-i", "$1" + "i-i");

        if (rand.nextInt(10) <= 2) {
            result += " >-<";
        }

        if (rand.nextInt(10) <= 1) {
            result += " UwU";
        }
        return result;
    }
}