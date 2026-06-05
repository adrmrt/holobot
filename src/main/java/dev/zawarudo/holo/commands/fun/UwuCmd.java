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
        final Message replyTarget;
        if (referenced != null) {
            ctx.invocation().deleteInvokeIfPossible();
            input = referenced.getContentRaw();
            replyTarget = referenced;
        } else {
            input = String.join(" ", args);
            replyTarget = invokingMessage;
        }

        final String uwu = uwuify(input);
        sendText(replyTarget, uwu);
    }

    private void sendText(Message target, String text) {
        int index = 0;
        while (index < text.length()) {
            String chunk = text.substring(
                    index,
                    Math.min(index + Message.MAX_CONTENT_LENGTH, text.length())
            );
            target.reply(chunk).queue();
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

        result = stutter(result);

        // 50% chance of adding a random emoji
        List<String> emoticons = List.of(" >-<", " >w<", " UwU", " OwO");
        if (rand.nextBoolean()) {
            result += emoticons.get(rand.nextInt(emoticons.size()));
        }
        return result;
    }

    // Each word has a 10% chance of being prefixed with its first letter and a dash, e.g. "hello" -> "h-hello"
    private static String stutter(String text) {
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() > 1 && Character.isLetter(word.charAt(0)) && rand.nextInt(10) == 0) {
                word = word.charAt(0) + "-" + word;
            }
            sb.append(word);
            if (i < words.length - 1) sb.append(" ");
        }
        return sb.toString();
    }
}