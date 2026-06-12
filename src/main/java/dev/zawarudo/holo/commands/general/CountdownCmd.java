package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.database.dao.CountdownDao;
import dev.zawarudo.holo.modules.countdown.Countdown;
import dev.zawarudo.holo.utils.DateTimeUtils;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.annotations.Deactivated;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Deactivated
@CommandInfo(name = "countdown",
    description = "Create, view and remove countdowns.",
    usage = "WIP",
    alias = {"cd"},
    category = CommandCategory.MISC
)
public class CountdownCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CountdownCmd.class);

    private final CountdownDao countdownDao;

    public CountdownCmd(CountdownDao countdownDao) {
        this.countdownDao = countdownDao;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        if (!ctx.hasArgs() || "list".equals(ctx.args().getFirst())) {
            showList(ctx);
            return;
        }

        String sub = ctx.args().getFirst().toLowerCase(Locale.ROOT);

        if ("add".equals(sub)) {
            if (ctx.argCount() < 3) {
                ctx.message().ifPresent(m -> m.reply(String.format("Usage: `%scountdown add <name> <date time>`", ctx.prefix().orElse(""))).queue());
                return;
            }
            createCountdown(ctx, ctx.args().get(1), joinFrom(ctx, 2));
            return;
        }

        if ("remove".equals(sub) || "r".equals(sub)) {
            if (ctx.argCount() < 2) {
                ctx.message().ifPresent(m -> m.reply(String.format("Usage: `%scountdown remove <id>`", ctx.prefix().orElse(""))).queue());
                return;
            }
            removeCountdown(ctx, ctx.args().get(1));
            return;
        }

        showCountdown(ctx);
    }

    private void showCountdown(CommandContext ctx) {
        try {
            long userId = ctx.user().getIdLong();
            long selectedId = Long.parseLong(ctx.args().getFirst());

            Optional<Countdown> selectedCountdown = countdownDao.findAllById(userId)
                .stream()
                .filter(cd -> cd.id() == selectedId)
                .findFirst();

            if (selectedCountdown.isPresent()) {
                Countdown cd = selectedCountdown.get();

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Countdown Information");
                embedBuilder.addField("ID", String.valueOf(cd.id()), false);
                embedBuilder.addField("Name", cd.name(), false);
                embedBuilder.addField("Date", DateTimeUtils.formatDateTime(cd.dateTime()), false);
                embedBuilder.addField("Remaining Time", Formatter.getRelativeTime(cd.dateTime()), false);
                embedBuilder.addField("Time Created", DateTimeUtils.formatDateTime(cd.timeCreated()), false);

                ctx.message().ifPresent(m -> m.replyEmbeds(embedBuilder.build()).queue());
            } else {
                ctx.message().ifPresent(m -> m.reply("You don't have a countdown with the given ID! Please check your list and try again.").queue());
            }
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            ctx.message().ifPresent(m -> m.reply("Something went wrong while working with the database.").queue());
        } catch (NumberFormatException _) {
            ctx.message().ifPresent(m -> m.reply("Please enter a valid countdown ID!").queue());
        }
    }

    private void showList(CommandContext ctx) {
        try {
            List<Countdown> countdowns = countdownDao.findAllById(ctx.user().getIdLong());
            StringBuilder sb = new StringBuilder();
            for (Countdown cd : countdowns) {
                sb.append("* ").append(String.format("**%s** ", cd.name())).append(String.format("`[ID: %d]`", cd.id())).append("\n")
                    .append(DateTimeUtils.formatDateTime(cd.dateTime())).append("\n")
                    .append(Formatter.getRelativeTime(cd.dateTime())).append("\n");
            }

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Your Countdowns");
            embedBuilder.setDescription(sb.isEmpty() ? "Your list is empty." : sb.toString());

            ctx.message().ifPresent(m -> m.replyEmbeds(embedBuilder.build()).queue());
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            ctx.message().ifPresent(m -> m.reply("Something went wrong while fetching your countdowns.").queue());
        }
    }

    private void createCountdown(CommandContext ctx, String name, String input) {
        try {
            long created = System.currentTimeMillis();
            long millis = DateTimeUtils.parseDateTime(input);
            String dateTime = DateTimeUtils.formatDateTime(millis);

            Countdown countdown = new Countdown(-1, name, created, millis, ctx.user().getIdLong(), ctx.guild().orElseThrow().getIdLong());
            countdownDao.insertIgnore(countdown);

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Created countdown");
            embedBuilder.addField("Name", name, false);
            embedBuilder.addField("Date", dateTime, false);
            embedBuilder.addField("Remaining time", Formatter.getRelativeTime(millis), false);

            ctx.message().ifPresent(m -> m.replyEmbeds(embedBuilder.build()).queue());
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            ctx.message().ifPresent(m -> m.reply("Something went wrong while storing your countdown.").queue());
        } catch (IllegalArgumentException _) {
            ctx.message().ifPresent(m -> m.reply("I can't parse your given date and/or time! Make sure you didn't make a typo and try again.").queue());
        }
    }

    private void removeCountdown(CommandContext ctx, String rawId) {
        try {
            long userId = ctx.user().getIdLong();
            long selectedId = Long.parseLong(rawId);

            Optional<Countdown> selectedCountdown = countdownDao.findAllById(userId)
                .stream()
                .filter(cd -> cd.id() == selectedId)
                .findFirst();

            if (selectedCountdown.isPresent()) {
                countdownDao.deleteIgnore(selectedCountdown.get().id());
                ctx.message().ifPresent(m -> m.reply("Successfully removed your countdown.").queue());
            } else {
                ctx.message().ifPresent(m -> m.reply("You don't have a countdown with the given ID! Please check your list and try again.").queue());
            }
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            ctx.message().ifPresent(m -> m.reply("Something went wrong while working with the database.").queue());
        } catch (NumberFormatException _) {
            ctx.message().ifPresent(m -> m.reply("Please enter a valid countdown ID!").queue());
        }
    }

    private static String joinFrom(CommandContext ctx, int startIdx) {
        if (startIdx >= ctx.argCount()) return "";
        return String.join(" ", ctx.args().subList(startIdx, ctx.args().size())).trim();
    }
}
