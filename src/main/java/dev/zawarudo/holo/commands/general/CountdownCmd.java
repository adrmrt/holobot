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
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    private static final int EPHEMERAL_REPLY_DELETE_MINUTES = 1;
    private static final int CREATED_COUNTDOWN_DELETE_MINUTES = 5;

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
                sendError(ctx, String.format("Usage: `%scountdown remove <id>`", ctx.prefix().orElse("")));
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
            sendError(ctx, addUsage(ctx));
            return;
        }

        Optional<AddArgs> parsedArgs = parseAddArgs(args);
        if (parsedArgs.isEmpty()) {
            sendError(ctx, addUsage(ctx));
            return;
        }
        AddArgs addArgs = parsedArgs.get();

        if (addArgs.visibility() == Countdown.Visibility.GLOBAL && !ctx.isGuildAdmin() && !ctx.isBotOwner()) {
            sendError(ctx, "Only server admins can create global countdowns!");
            return;
        }
        if (addArgs.visibility() != Countdown.Visibility.PRIVATE && ctx.guild().isEmpty()) {
            sendError(ctx, "Public and global countdowns can only be created in a server!");
            return;
        }

        createCountdown(ctx, addArgs.name(), addArgs.dateInput(), addArgs.visibility());
    }

    private String addUsage(CommandContext ctx) {
        return String.format("""
            Usage: `%scountdown add [global|public|private] <name> <date/time>`
            - `private` (default): only you can see it, via `countdown list` or `countdown <id>`
            - `public`: anyone in the server can view it via `countdown <id>`; `countdown list` still only shows your own
            - `global`: visible to everyone in the server via `countdown all` or `countdown <id>` (admin-only to create)""",
            ctx.prefix().orElse(""));
    }

    /**
     * The name and date/time portion of a {@code countdown add} invocation, once the leading
     * {@code add} token and optional visibility keyword have been separated out. Args are expected to
     * already be tokenized (quoted phrases grouped into one entry) by {@link dev.zawarudo.holo.commands.CommandListener}.
     */
    record AddArgs(Countdown.Visibility visibility, String name, String dateInput) {
    }

    /**
     * Splits {@code args} (starting with "add") into visibility, name, and the remaining date/time
     * string. Returns empty if there aren't enough tokens left for both a name and a date.
     */
    static Optional<AddArgs> parseAddArgs(List<String> args) {
        Countdown.Visibility visibility = Countdown.Visibility.PRIVATE;
        int nameIdx = 1;

        Optional<Countdown.Visibility> parsed = parseVisibility(args.get(1).toLowerCase(Locale.ROOT));
        if (parsed.isPresent()) {
            visibility = parsed.get();
            nameIdx = 2;
        }

        if (args.size() < nameIdx + 2) {
            return Optional.empty();
        }

        String name = args.get(nameIdx);
        String dateInput = String.join(" ", args.subList(nameIdx + 1, args.size()));
        return Optional.of(new AddArgs(visibility, name, dateInput));
    }

    private static Optional<Countdown.Visibility> parseVisibility(String value) {
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
            if (selectedCountdown.isEmpty() || !selectedCountdown.get().isVisibleTo(ctx.user().getIdLong(), ctx.guildIdOrZero())) {
                sendError(ctx, "You don't have a countdown with the given ID! Please check your list and try again.");
                return;
            }
            Countdown cd = selectedCountdown.get();

            EmbedBuilder builder = newEmbed("Countdown Information");
            builder.addField("ID", String.valueOf(cd.id()), true);
            builder.addField("Visibility", cd.visibility().name(), true);
            builder.addField("Name", cd.name(), false);
            builder.addField("Date", DiscordTimestamp.LONG_DATE_TIME.getTimestamp(cd.dateTime()), false);
            builder.addField("Remaining Time", DiscordTimestamp.RELATIVE_TIME.getTimestamp(cd.dateTime()), false);
            builder.addField("Time Created", DiscordTimestamp.LONG_DATE_TIME.getTimestamp(cd.timeCreated()), false);

            ctx.reply().embed(builder);
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            sendError(ctx, "Something went wrong while working with the database.");
        } catch (NumberFormatException _) {
            sendError(ctx, "Please enter a valid countdown ID!");
        }
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

            EmbedBuilder builder = newEmbed("Your Countdowns");
            builder.setDescription(sb.isEmpty() ? "Your list is empty." : sb.toString());

            ctx.reply().embed(builder);
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            sendError(ctx, "Something went wrong while fetching your countdowns.");
        }
    }

    private void showGlobalList(CommandContext ctx) {
        if (ctx.guild().isEmpty()) {
            sendError(ctx, "Global countdowns can only be viewed in a server!");
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

            EmbedBuilder builder = newEmbed("Server Countdowns");
            builder.setDescription(sb.isEmpty() ? "There are no global countdowns in this server." : sb.toString());

            ctx.reply().embed(builder);
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            sendError(ctx, "Something went wrong while fetching the server's countdowns.");
        }
    }

    private void createCountdown(CommandContext ctx, String name, String input, Countdown.Visibility visibility) {
        try {
            long created = System.currentTimeMillis();
            long millis = DateTimeUtils.parseDateTime(input, ctx.referenceZone());

            long guildId = ctx.guildIdOrZero();
            long channelId = ctx.channel().getIdLong();

            Countdown countdown = new Countdown(-1, name, created, millis, ctx.user().getIdLong(), guildId,
                visibility, channelId, false);
            long id = countdownManager.createCountdown(countdown);

            EmbedBuilder builder = newEmbed("Created Countdown");
            builder.addField("ID", String.valueOf(id), true);
            builder.addField("Visibility", visibility.name(), true);
            builder.addField("Name", name, false);
            builder.addField("Date", DiscordTimestamp.LONG_DATE_TIME.getTimestamp(millis), false);
            builder.addField("Remaining Time", DiscordTimestamp.RELATIVE_TIME.getTimestamp(millis), false);

            ctx.reply().embedAndDeleteInvoke(ctx, builder.build(), CREATED_COUNTDOWN_DELETE_MINUTES, TimeUnit.MINUTES);
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            sendError(ctx, "Something went wrong while storing your countdown.");
        } catch (IllegalArgumentException _) {
            sendError(ctx, Formatter.dateParseErrorHint(ctx.prefix().orElse("")));
        }
    }

    private void removeCountdown(CommandContext ctx, String rawId) {
        try {
            long selectedId = Long.parseLong(rawId);

            Optional<Countdown> selectedCountdown = countdownManager.findById(selectedId);
            if (selectedCountdown.isEmpty() || selectedCountdown.get().userId() != ctx.user().getIdLong()) {
                sendError(ctx, "You don't have a countdown with the given ID! Please check your list and try again.");
                return;
            }

            countdownManager.removeCountdown(selectedId);

            MessageEmbed embed = newEmbed("Countdown Removed")
                .setDescription("Successfully removed your countdown.")
                .build();
            ctx.reply().embedAndDeleteInvoke(ctx, embed, EPHEMERAL_REPLY_DELETE_MINUTES, TimeUnit.MINUTES);
        } catch (SQLException e) {
            LOGGER.error("Something went wrong", e);
            sendError(ctx, "Something went wrong while working with the database.");
        } catch (NumberFormatException _) {
            sendError(ctx, "Please enter a valid countdown ID!");
        }
    }

    /**
     * Sends a pre-styled error embed and deletes both it and the invoking message after
     * {@link #EPHEMERAL_REPLY_DELETE_MINUTES} minute(s), to avoid cluttering the channel with
     * transient usage/validation errors.
     */
    private void sendError(CommandContext ctx, String content) {
        MessageEmbed embed = new EmbedBuilder()
            .setTitle("Error")
            .setDescription(content)
            .setColor(EmbedColor.ERROR.getColor())
            .build();
        ctx.reply().embedAndDeleteInvoke(ctx, embed, EPHEMERAL_REPLY_DELETE_MINUTES, TimeUnit.MINUTES);
    }
}
