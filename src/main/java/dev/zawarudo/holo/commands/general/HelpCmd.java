package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.commands.CommandManager;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CommandInfo(name = "help",
    description = "Shows a list of commands or their respective usage",
    usage = "[command]",
    example = "ping",
    guildOnly = false,
    category = CommandCategory.GENERAL)
public class HelpCmd extends AbstractCommand implements ExecutableCommand {

    private final CommandManager manager;

    /**
     * Creates a new instance of the help command.
     *
     * @param manager The command manager that will be used to retrieve commands
     *                and their respective information.
     */
    public HelpCmd(CommandManager manager) {
        this.manager = manager;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        // Send the full help page
        if (!ctx.hasArgs()) {
            sendHelpPage(ctx);
            return;
        }

        String query = ctx.args().getFirst().toLowerCase(Locale.ROOT);

        // Given command doesn't exist
        if (!manager.isValidName(query)) {
            sendCommandNotFound(ctx, query);
            return;
        }

        // Help page for given command
        sendHelpPageForCommand(ctx, manager.getCommand(query));
    }

    private void sendCommandNotFound(CommandContext ctx, String query) {
        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Command not found")
            .setDescription("Please check for typos and try again!")
            .addField("Tried", Formatter.asCodeBlock(query), false);

        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl()));

        ctx.reply().embed(builder.build(), 15, TimeUnit.SECONDS);
    }

    private void sendHelpPage(CommandContext ctx) {
        String prefix = ctx.prefix().orElse("");

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Help Page")
            .setThumbnail(ctx.jda().getSelfUser().getEffectiveAvatarUrl().concat("?size=512"))
            .setDescription(
                "I currently use `" + prefix + "` as prefix for all commands.\n" +
                    "For more information on a certain command, use " +
                    Formatter.asCodeBlock(prefix + "help <command>")
            );

        for (CommandCategory category : CommandCategory.values()) {
            List<AbstractCommand> visible = getVisibleCommands(category, ctx);

            if (visible.isEmpty()) {
                continue;
            }

            String names = visible.stream()
                .map(AbstractCommand::getName)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.joining(", "));

            builder.addField(category.getName(), Formatter.asCodeBlock(names), false);
        }

        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl()));

        ctx.reply().embed(builder.build(), 2, TimeUnit.MINUTES);
    }

    private List<AbstractCommand> getVisibleCommands(CommandCategory category, CommandContext ctx) {
        if (!canSeeCategory(category, ctx)) {
            return List.of();
        }

        boolean isGuild = ctx.inGuild();
        boolean isOwner = ctx.isBotOwner();
        boolean isAdmin = isGuild && ctx.isGuildAdmin();

        return manager.getCommands(category).stream()
            // Hide guild-only commands in DMs
            .filter(cmd -> isGuild || !cmd.isGuildOnly())

            // Hide owner-only commands
            .filter(cmd -> !cmd.isOwnerOnly() || isOwner)

            // Hide admin-only commands
            .filter(cmd -> !cmd.isAdminOnly() || isAdmin || isOwner)

            .toList();
    }

    private boolean canSeeCategory(CommandCategory category, CommandContext ctx) {
        return switch (category) {
            case OWNER -> ctx.isBotOwner();
            case ADMIN -> ctx.isBotOwner() || ctx.isGuildAdmin();
            default -> true;
        };
    }

    /**
     * Sends the help page for a given command.
     */
    private void sendHelpPageForCommand(CommandContext ctx, AbstractCommand cmd) {
        String prefix = ctx.prefix().orElse("");

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Command Help")
            .addField("Name", Formatter.asCodeBlock(cmd.getName()), false)
            .addField("Description", cmd.getDescription(), false);

        if (cmd.hasUsage()) {
            builder.addField(
                "Usage",
                Formatter.asCodeBlock(prefix + cmd.getName() + " " + cmd.getUsage()),
                false);
        }

        if (cmd.hasExample()) {
            builder.addField(
                "Example",
                Formatter.asCodeBlock(prefix + cmd.getName() + " " + cmd.getExample()),
                false);
        }

        if (cmd.hasAlias()) {
            String aliases = String.join(", ", cmd.getAlias());
            builder.addField("Aliases", Formatter.asCodeBlock(aliases), false);
        }

        if (cmd.hasThumbnail()) {
            builder.setThumbnail(cmd.getThumbnail());
        }

        builder.setColor(cmd.getEmbedColor());
        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl()));

        ctx.reply().embed(builder.build(), 1, TimeUnit.MINUTES);
    }
}
