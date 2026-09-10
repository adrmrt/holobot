package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.DateTimeUtils;
import dev.zawarudo.holo.utils.DiscordTimestamp;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@CommandInfo(name = "timestamp",
    description = "Converts a date and/or time into every Discord dynamic timestamp style, ready to copy and paste.",
    usage = "<date and/or time> | formats",
    example = "25/12/2026 18:00",
    alias = {"ts"},
    category = CommandCategory.GENERAL,
    guildOnly = false
)
public class TimestampCmd implements CommandMetadata, ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        if (!ctx.hasArgs()) {
            ctx.reply().errorEmbed(Formatter.dateParseErrorHint(ctx.prefix().orElse("")));
            return;
        }

        if ("formats".equals(ctx.args().getFirst().toLowerCase(Locale.ROOT))) {
            showFormats(ctx);
            return;
        }

        long millis;
        try {
            millis = DateTimeUtils.parseDateTime(ctx.argString());
        } catch (IllegalArgumentException e) {
            ctx.reply().errorEmbed(Formatter.dateParseErrorHint(ctx.prefix().orElse("")));
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(getEmbedColor());
        builder.setTitle("Discord Timestamps");
        builder.setDescription("Here's your date and/or time in every Discord timestamp style. Click a field to copy its code.");

        for (DiscordTimestamp style : DiscordTimestamp.values()) {
            String code = style.getTimestamp(millis);
            builder.addField(styleName(style), String.format("%s\n`%s`", code, code), false);
        }

        ctx.reply().embed(builder);
    }

    private void showFormats(CommandContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (String example : DateTimeUtils.FORMAT_EXAMPLES) {
            sb.append("`").append(example).append("`\n");
        }
        sb.append("\nAny of these also accepts a trailing offset, e.g. `(UTC+8)` or `(UTC-4)`.");

        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(getEmbedColor());
        builder.setTitle("Supported Date/Time Formats");
        builder.setDescription(sb.toString());

        ctx.reply().embed(builder);
    }

    private static String styleName(DiscordTimestamp style) {
        return switch (style) {
            case DEFAULT -> "Default";
            case SHORT_TIME -> "Short Time";
            case LONG_TIME -> "Long Time";
            case SHORT_DATE -> "Short Date";
            case LONG_DATE -> "Long Date";
            case SHORT_DATE_TIME -> "Short Date/Time";
            case LONG_DATE_TIME -> "Long Date/Time";
            case RELATIVE_TIME -> "Relative";
        };
    }
}
