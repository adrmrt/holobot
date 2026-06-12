package dev.zawarudo.holo.commands.owner;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import org.jspecify.annotations.NonNull;

import java.util.List;

@CommandInfo(name = "echo",
    description = "Repeats a given message by a given amount.",
    usage = "<amount> <message>",
    alias = {"say", "repeat"},
    ownerOnly = true,
    category = CommandCategory.OWNER)
public class EchoCmd implements CommandMetadata, ExecutableCommand {

    private static final int MIN_TIMES = 1;
    private static final int MAX_TIMES = 100;

    @Override
    public void execute(@NonNull CommandContext ctx) {
        final List<String> args = ctx.args();

        String prefix = ctx.prefix().orElse("");
        String usage = "`" + prefix + ctx.invokedAs() + " [<amount>] <message>`";

        if (args.isEmpty()) {
            ctx.reply().errorEmbed("Usage: " + usage);
            return;
        }

        int times = 1;
        String sentence;

        Integer parsedTimes = tryParseInt(args.getFirst());
        if (parsedTimes != null) {
            times = parsedTimes;

            if (args.size() < 2) {
                ctx.reply().errorEmbed("Usage: " + usage);
                return;
            }

            sentence = String.join(" ", args.subList(1, args.size())).trim();
        } else {
            // No number -> repeat just once
            sentence = String.join(" ", args).trim();
        }

        if (times < MIN_TIMES || times > MAX_TIMES) {
            ctx.reply().errorEmbed("`amount` must be between " + MIN_TIMES + " and " + MAX_TIMES + ".");
            return;
        }

        if (sentence.isEmpty()) {
            ctx.reply().errorEmbed("Message must not be empty.\nUsage: " + usage);
            return;
        }

        final String content = sentence.replace("\\n", "\n");

        for (int i = 0; i < times; i++) {
            ctx.channel().sendMessage(content).queue();
        }
    }

    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException _) {
            return null;
        }
    }
}
