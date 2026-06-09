package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.commands.CommandManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
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
public class HelpCmd extends AbstractCommand {

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
    public void onCommand(@NotNull MessageReceivedEvent event) {
        deleteInvoke(event);

        // Send the full help page
        if (args.length == 0) {
            sendHelpPage(event);
            return;
        }

        String query = args[0].toLowerCase(Locale.ROOT);

        // Given command doesn't exist
        if (!manager.isValidName(query)) {
            sendCommandNotFound(event, query);
            return;
        }

        // Help page for given command
        sendHelpPageForCommand(event, manager.getCommand(args[0]));
    }

    private void sendCommandNotFound(MessageReceivedEvent event, String query) {
        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Command not found")
            .setDescription("Please check for typos and try again!")
            .addField("Tried", Formatter.asCodeBlock(query), false);

        sendEmbed(event, builder, true, 15, TimeUnit.SECONDS);
    }

    private void sendHelpPage(MessageReceivedEvent event) {
        String prefix = getPrefix(event);

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Help Page")
            .setThumbnail(event.getJDA().getSelfUser().getEffectiveAvatarUrl().concat("?size=512"))
            .setDescription(
                "I currently use `" + prefix + "` as prefix for all commands.\n" +
                    "For more information on a certain command, use " +
                    Formatter.asCodeBlock(prefix + "help <command>")
            );

        for (CommandCategory category : CommandCategory.values()) {
            List<AbstractCommand> visible = getVisibleCommands(category, event);

            if (visible.isEmpty()) {
                continue;
            }

            String names = visible.stream()
                .map(AbstractCommand::getName)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.joining(", "));

            builder.addField(category.getName(), Formatter.asCodeBlock(names), false);
        }
        sendEmbed(event, builder, true, 2, TimeUnit.MINUTES);
    }

    private List<AbstractCommand> getVisibleCommands(CommandCategory category, MessageReceivedEvent event) {
        if (!canSeeCategory(category, event)) {
            return List.of();
        }

        boolean isGuild = event.isFromGuild();
        boolean isOwner = isBotOwner(event.getAuthor());
        boolean isAdmin = isGuild && isGuildAdmin(event);

        return manager.getCommands(category).stream()
            // Hide guild-only commands in DMs
            .filter(cmd -> isGuild || !cmd.isGuildOnly())

            // Hide owner-only commands
            .filter(cmd -> !cmd.isOwnerOnly() || isOwner)

            // Hide admin-only commands
            .filter(cmd -> !cmd.isAdminOnly() || isAdmin || isOwner)

            .toList();
    }

    private boolean canSeeCategory(CommandCategory category, MessageReceivedEvent event) {
        return switch (category) {
            case OWNER -> isBotOwner(event.getAuthor());
            case ADMIN -> isBotOwner(event.getAuthor()) || isGuildAdmin(event);
            default -> true;
        };
    }

    /**
     * Sends the help page for a given command.
     */
    private void sendHelpPageForCommand(MessageReceivedEvent event, AbstractCommand cmd) {
        String prefix = getPrefix(event);

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

        sendEmbed(event, builder, true, 1, TimeUnit.MINUTES, cmd.getEmbedColor());
    }
}
