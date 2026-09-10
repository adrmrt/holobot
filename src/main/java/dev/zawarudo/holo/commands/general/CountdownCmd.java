package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.modules.countdown.Countdown;
import dev.zawarudo.holo.modules.countdown.CountdownManager;
import dev.zawarudo.holo.utils.DateTimeUtils;
import dev.zawarudo.holo.utils.DiscordTimestamp;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@CommandInfo(name = "countdown",
    description = "Create, view and remove countdowns.",
    usage = "add [global|public|private] <name> <date/time> | list | all | <id> | remove <id>",
    example = "add global NewYear 01/01/2027 00:00",
    alias = {"cd"},
    category = CommandCategory.MISC,
    embedColor = EmbedColor.COUNTDOWN,
    guildOnly = false
)
public class CountdownCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CountdownCmd.class);

    private final CountdownManager countdownManager;

    public CountdownCmd(CountdownManager countdownManager) {
        this.countdownManager = countdownManager;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        if (!ctx.hasArgs() || "list".equals(ctx.args().getFirst())) {
            showList(ctx);
            return;
        }

        String sub = ctx.args().getFirst().toLowerCase(Locale.ROOT);

        if ("add".equals(sub)) {
            handleAdd(ctx);
            return;
        }

        if ("all".equals(sub)) {
            showGlobalList(ctx);
            return;
        }

        if ("remove".equals(sub) || "r".equals(sub)) {
            if (ctx.argCount() < 2) {
                ctx.reply().errorEmbed(String.format("Usage: `%scountdown remove <id>`", ctx.prefix().orElse("")));
                return;
            }
            removeCountdown(ctx, ctx.args().get(1));
            return;
        }

        showCountdown(ctx, ctx.args().getFirst());
    }

    private void handleAdd(CommandContext ctx) {
        List<String> args = ctx.args();
        if (args.size() < 3) {
            ctx.reply().errorEmbed(String.format("Usage: `%scountdown add [global|public|private] <name> <date/time>`", ctx.prefix().orElse("")));
            return;
        }

        Countdown.Visibility visibility = Countdown.Visibility.PRIVATE;
        int nameIdx = 1;

        String maybeVisibility = args.get(1).toLowerCase(Locale.ROOT);
        Optional<Countdown.Visibility> parsed = parseVisibility(maybeVisibility);
        if (parsed.isPresent()) {
            visibility = parsed.get();
            nameIdx = 2;
        }

        if (visibility == Countdown.Visibility.GLOBAL && !ctx.isGuildAdmin()) {
            ctx.reply().errorEmbed("Only server admins can create global countdowns!");
            return;
        }
        if (visibility != Countdown.Visibility.PRIVATE && ctx.guild().isEmpty()) {
            ctx.reply().errorEmbed("Public and global countdowns can only be created in a server!");
            return;
        }

        if (args.size() < nameIdx + 2) {
            ctx.reply().errorEmbed(String.format("Usage: `%scountdown add [global|public|private] <name> <date/time>`", ctx.prefix().orElse("")));
            return;
        }

        String name = args.get(nameIdx);
        String dateInput = String.join(" ", args.subList(nameIdx + 1, args.size()));

        createCountdown(ctx, name, dateInput, visibility);
    }

    private Optional<Countdown.Visibility> parseVisibility(String value) {
        return switch (value) {
            case "global" -> Optional.of(Countdown.Visibility.GLOBAL);
            case "public" -> Optional.of(Countdown.Visibility.PUBLIC);
            case "private" -> Optional.of(Countdown.Visibility.PRIVATE);
            default -> Optional.empty();
        };
    }

    private void showCountdown(CommandContext ctx, String rawId) {
        try {
            long selectedId = Long.parseLong(rawId);

            Optional<Countdown> selectedCountdown = countdownManager.findById(selectedId);
            if (selectedCountdown.isEmpty() || !isVisibleTo(ctx, selectedCountdown.get())) {
                ctx.reply().errorEmbed("You don't have a countdown with the given ID! Please check your list and try again.");
                return;
            }
            Countdown cd = selectedCountdown.get();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(getEmbedColor());
            builder.setTitle("Countdown Information");
            builder.addField("ID", String.valueOf(cd.id()), true);
            builder.addField("Visibility", cd.visibility().name(), true);
            builder.addField("Name", cd.name(), false);
            builder.addField("Date", DiscordTimestamp.LONG_DATE_TIME.getTimestamp(cd.dateTime()), false);
            builder.addField("Remaining Time", DiscordTimestamp.RELATIVE_TIME.getTimestamp(cd.dateTime()), false);
            builder.addField("Time Created", DiscordTimestamp.LONG_DATE_TIME.getTimestamp(cd.timeCreated()), false);

            ctx.reply().embed(builder);
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            ctx.reply().errorEmbed("Something went wrong while working with the database.");
        } catch (NumberFormatException _) {
            ctx.reply().errorEmbed("Please enter a valid countdown ID!");
        }
    }

    private boolean isVisibleTo(CommandContext ctx, Countdown cd) {
        if (cd.userId() == ctx.user().getIdLong()) {
            return true;
        }
        if (cd.visibility() == Countdown.Visibility.GLOBAL) {
            return ctx.guild().map(g -> g.getIdLong() == cd.guildId()).orElse(false);
        }
        return false;
    }

    private void showList(CommandContext ctx) {
        try {
            List<Countdown> countdowns = countdownManager.listFor(ctx.user().getIdLong());
            StringBuilder sb = new StringBuilder();
            for (Countdown cd : countdowns) {
                sb.append("* ").append(String.format("**%s** ", cd.name()))
                    .append(String.format("`[ID: %d, %s]`", cd.id(), cd.visibility())).append("\n")
                    .append(DiscordTimestamp.SHORT_DATE_TIME.getTimestamp(cd.dateTime())).append(" — ")
                    .append(DiscordTimestamp.RELATIVE_TIME.getTimestamp(cd.dateTime())).append("\n");
            }

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(getEmbedColor());
            builder.setTitle("Your Countdowns");
            builder.setDescription(sb.isEmpty() ? "Your list is empty." : sb.toString());

            ctx.reply().embed(builder);
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            ctx.reply().errorEmbed("Something went wrong while fetching your countdowns.");
        }
    }

    private void showGlobalList(CommandContext ctx) {
        if (ctx.guild().isEmpty()) {
            ctx.reply().errorEmbed("Global countdowns can only be viewed in a server!");
            return;
        }

        try {
            List<Countdown> countdowns = countdownManager.listGlobal(ctx.guild().get().getIdLong());
            StringBuilder sb = new StringBuilder();
            for (Countdown cd : countdowns) {
                sb.append("* ").append(String.format("**%s** ", cd.name()))
                    .append(String.format("`[ID: %d]`", cd.id())).append("\n")
                    .append(DiscordTimestamp.SHORT_DATE_TIME.getTimestamp(cd.dateTime())).append(" — ")
                    .append(DiscordTimestamp.RELATIVE_TIME.getTimestamp(cd.dateTime())).append("\n");
            }

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(getEmbedColor());
            builder.setTitle("Server Countdowns");
            builder.setDescription(sb.isEmpty() ? "There are no global countdowns in this server." : sb.toString());

            ctx.reply().embed(builder);
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            ctx.reply().errorEmbed("Something went wrong while fetching the server's countdowns.");
        }
    }

    private void createCountdown(CommandContext ctx, String name, String input, Countdown.Visibility visibility) {
        try {
            long created = System.currentTimeMillis();
            ZoneId zone = ctx.guildConfig().map(gc -> ZoneId.of(gc.getTimezone())).orElse(ZoneId.systemDefault());
            long millis = DateTimeUtils.parseDateTime(input, zone);

            long guildId = ctx.guild().map(g -> g.getIdLong()).orElse(0L);
            long channelId = ctx.channel().getIdLong();

            Countdown countdown = new Countdown(-1, name, created, millis, ctx.user().getIdLong(), guildId,
                visibility, channelId, false);
            long id = countdownManager.createCountdown(countdown);

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(getEmbedColor());
            builder.setTitle("Created Countdown");
            builder.addField("ID", String.valueOf(id), true);
            builder.addField("Visibility", visibility.name(), true);
            builder.addField("Name", name, false);
            builder.addField("Date", DiscordTimestamp.LONG_DATE_TIME.getTimestamp(millis), false);
            builder.addField("Remaining Time", DiscordTimestamp.RELATIVE_TIME.getTimestamp(millis), false);

            ctx.reply().embed(builder);
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            ctx.reply().errorEmbed("Something went wrong while storing your countdown.");
        } catch (IllegalArgumentException _) {
            ctx.reply().errorEmbed("I can't parse your given date and/or time! Make sure you didn't make a typo and try again.");
        }
    }

    private void removeCountdown(CommandContext ctx, String rawId) {
        try {
            long selectedId = Long.parseLong(rawId);

            Optional<Countdown> selectedCountdown = countdownManager.findById(selectedId);
            if (selectedCountdown.isEmpty() || selectedCountdown.get().userId() != ctx.user().getIdLong()) {
                ctx.reply().errorEmbed("You don't have a countdown with the given ID! Please check your list and try again.");
                return;
            }

            countdownManager.removeCountdown(selectedId);
            ctx.reply().text("Successfully removed your countdown.");
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            ctx.reply().errorEmbed("Something went wrong while working with the database.");
        } catch (NumberFormatException _) {
            ctx.reply().errorEmbed("Please enter a valid countdown ID!");
        }
    }
}
