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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "datecheck",
    description = "Shows the weekday, relative time, and optional timezone conversion for a given date and/or time.",
    usage = "<date and/or time> [timezone]",
    example = "25/12/2026 [Europe/Zurich]",
    alias = {"dc"},
    category = CommandCategory.GENERAL,
    guildOnly = false
)
public class DateCheckCmd implements CommandMetadata, ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        if (!ctx.hasArgs()) {
            ctx.reply().errorEmbed(Formatter.dateParseErrorHint(ctx.prefix().orElse("")));
            return;
        }

        List<String> args = ctx.args();
        String timeZoneId = null;
        String dateInput = ctx.argString();

        if (args.size() > 1 && DateTimeUtils.isValidTimeZone(args.getLast())) {
            timeZoneId = args.getLast();
            dateInput = String.join(" ", args.subList(0, args.size() - 1));
        }

        long millis;
        try {
            millis = DateTimeUtils.parseDateTime(dateInput);
        } catch (IllegalArgumentException e) {
            ctx.reply().errorEmbed(Formatter.dateParseErrorHint(ctx.prefix().orElse("")));
            return;
        }

        ZoneId zone = timeZoneId != null ? ZoneId.of(timeZoneId) : ZoneId.systemDefault();
        String weekday = DateTimeUtils.getWeekDayFromDate(millis, zone);

        long diff = millis - System.currentTimeMillis();
        long daysDiff = TimeUnit.MILLISECONDS.toDays(Math.abs(diff));
        String dayCount = daysDiff == 1 ? "1 day" : daysDiff + " days";
        String direction = diff >= 0 ? dayCount + " from now" : dayCount + " ago";

        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(getEmbedColor());
        builder.setTitle("Date Check");
        builder.addField("Weekday", weekday, true);
        builder.addField("Relative", DiscordTimestamp.RELATIVE_TIME.getTimestamp(millis) + " (" + direction + ")", true);
        builder.addField("Full Date", DiscordTimestamp.LONG_DATE_TIME.getTimestamp(millis), false);

        if (timeZoneId != null) {
            ZonedDateTime converted = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zone);
            String formatted = converted.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT));
            builder.addField("In " + timeZoneId, formatted, false);
        }

        ctx.reply().embed(builder);
    }
}
