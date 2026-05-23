package dev.zawarudo.holo.commands;

import dev.zawarudo.holo.commands.fun.EmoteCmd;
import dev.zawarudo.holo.commands.image.ActionCmd;
import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.core.PermissionManager;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.CommandContextFactory;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.Formatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.zawarudo.holo.utils.LoggingUtils.withMdc;

/**
 * Class that listens to messages and checks if a bot command has been called. If that's
 * the case, it executes the command with the given arguments.
 */
public class CommandListener extends ListenerAdapter {

    private final CommandManager cmdManager;
    private final PermissionManager permManager;

    private final ExecutorService executorService;

    private final CommandContextFactory ctxFactory;

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandListener.class);
    private static final Logger AUDIT = LoggerFactory.getLogger("audit");

    public CommandListener(CommandManager cmdManager, PermissionManager permManager, ExecutorService executor, CommandContextFactory ctxFactory) {
        this.cmdManager = cmdManager;
        this.permManager = permManager;
        this.executorService = executor;
        this.ctxFactory = ctxFactory;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore webhooks and bots
        if (event.isWebhookMessage() || event.getAuthor().isBot()) {
            return;
        }

        final String prefix = getPrefix(event);
        final String rawMsg = event.getMessage().getContentRaw();

        // Ignore messages without the bot prefix
        if (!rawMsg.startsWith(prefix) || rawMsg.equals(prefix)) {
            return;
        }

        String withoutPrefix = rawMsg.replaceFirst("(?i)" + Pattern.quote(prefix), "");
        List<String> split = parseArguments(withoutPrefix);
        if (split.isEmpty()) {
            return;
        }

        String invoke = split.getFirst().toLowerCase(Locale.ROOT);

        // Build MDC context for logging
        Map<String, String> mdc = new HashMap<>();
        mdc.put("guildId", event.isFromGuild() ? event.getGuild().getId() : "DM");
        mdc.put("channelId", event.getChannel().getId());
        mdc.put("userId", event.getAuthor().getId());
        mdc.put("command", cmdManager.isValidName(invoke) ? cmdManager.getCommand(invoke).getName() : invoke);

        // Action cmd has been called
        ActionCmd actionCmd = (ActionCmd) cmdManager.getCommand("action");
        if (actionCmd.isAction(invoke)) {

            // Logging
            MDC.setContextMap(mdc);
            try {
                AUDIT.info("Action command invoked");
                LOGGER.info("Action command invoked");
            } finally {
                MDC.clear();
            }

            String[] directedArgs = (split.size() > 1)
                    ? split.subList(1, split.size()).toArray(new String[0])
                    : new String[0];

            actionCmd.displayAction(event, actionCmd.getAction(invoke), directedArgs);
            return;
        }

        // No valid command
        if (!cmdManager.isValidName(invoke)) {
            checkEmoteInvoke(event, invoke);
            return;
        }

        AbstractCommand cmd = cmdManager.getCommand(invoke);

        // Module disabled check (guild-only)
        if (event.isFromGuild()) {
            Optional<CommandModule.ModuleId> moduleIdOpt = cmdManager.getModuleOf(cmd);

            if (moduleIdOpt.isPresent()) {
                CommandModule.ModuleId moduleId = moduleIdOpt.get();
                var cfg = Bootstrap.holo.getGuildConfigManager().getOrCreate(event.getGuild());

                if (!cfg.isModuleEnabled(moduleId)) {
                    executorService.submit(withMdc(mdc, () ->
                            event.getChannel()
                                    .sendMessage("This command module is disabled on this server: `" + moduleId.id() + "`")
                                    .queue()
                    ));
                    return;
                }
            }
        }

        var decision = permManager.check(event, cmd);
        if (!decision.allowed()) {
            permManager.respondDenied(event, decision);
            return;
        }

        // Log the call with MDC
        MDC.setContextMap(mdc);
        try {
            AUDIT.info("Command invoked");
            LOGGER.info("Command invoked");
        } finally {
            MDC.clear();
        }

        List<String> argList = (split.size() > 1) ? split.subList(1, split.size()) : List.of();

        CommandContext ctx = ctxFactory.createForMessage(
                event,
                cmd.getName(),
                invoke,
                argList,
                prefix
        );

        executorService.submit(withMdc(mdc, () -> {
            try {
                if (cmd instanceof ExecutableCommand cc) {
                    cc.execute(ctx);
                } else {
                    cmd.args = argList.toArray(new String[0]);
                    cmd.onCommand(event);
                }
            } catch (InsufficientPermissionException ex) {
                handlePermissionError(event, ex);
            } catch (Exception ex) {
                LOGGER.error("An error occurred while executing a command.", ex);
            }
        }));
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        // TODO: Slash command invocations
    }

    private String getPrefix(MessageReceivedEvent e) {
        if (e.isFromGuild()) {
            return Bootstrap.holo.getGuildConfigManager()
                    .getOrCreate(e.getGuild())
                    .getPrefix();
        }

        return Bootstrap.holo.getConfig().getDefaultPrefix();
    }

    /**
     * Parses the given input string into a list of arguments, recognizing spaces as delimiters. Arguments
     * enclosed in double quotes are treated as single arguments.
     */
    private List<String> parseArguments(String input) {
        List<String> arguments = new ArrayList<>();
        Pattern pattern = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                arguments.add(matcher.group(1));
            } else {
                arguments.add(matcher.group());
            }
        }
        return arguments;
    }

    private void handlePermissionError(MessageReceivedEvent event, InsufficientPermissionException ex) {
        String permissionName = ex.getPermission().getName();

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Missing Permission");
        builder.setDescription("Cannot perform action due to a lack of permission. Please update my permissions " +
                "so I can run the called command.");
        builder.addField("Permission", Formatter.asCodeBlock(permissionName), false);

        boolean hasWritePermission = PermissionUtil.checkPermission(
                event.getGuildChannel().getPermissionContainer(),
                event.getGuild().getSelfMember(),
                Permission.MESSAGE_SEND
        );

        if (hasWritePermission) {
            event.getChannel().sendMessage(event.getAuthor().getAsMention()).addEmbeds(builder.build()).queue();
            return;
        }

        String serverId = event.getGuild().getId();
        String channelId = event.getChannel().getId();
        String messageId = event.getMessageId();

        String channelLink = String.format("https://discord.com/channels/%s/%s", serverId, channelId);
        builder.addField("Channel", channelLink, false);
        String messageLink = String.format("%s/%s", channelLink, messageId);
        builder.addField("Message", messageLink, false);

        event.getAuthor().openPrivateChannel().queue(dm -> dm.sendMessageEmbeds(builder.build()).queue(s -> {
                },
                err -> LOGGER.warn("Can't send a private message because I have been blocked by {} (ID: {}).",
                        event.getAuthor().getName(), event.getAuthor().getId())));
    }

    private void checkEmoteInvoke(MessageReceivedEvent event, String invoke) {
        try {
            // Only guild channels support webhooks
            if (!event.isFromGuild()) {
                return;
            }

            // Check MANAGE_WEBHOOKS permission
            if (!PermissionUtil.checkPermission(
                    event.getGuildChannel().getPermissionContainer(),
                    event.getGuild().getSelfMember(),
                    Permission.MANAGE_WEBHOOKS)) {
                return;
            }

            Optional<CustomEmoji> emoteOptional = Bootstrap.holo.getEmoteManager().getEmoteByName(invoke);
            if (emoteOptional.isPresent()) {
                CustomEmoji emote = emoteOptional.get();
                LOGGER.info("{} has called emote ({})", event.getAuthor().getName(), invoke);
                EmoteCmd cmd = (EmoteCmd) cmdManager.getCommand("emote");
                cmd.sendEmoteMessage(event, emote);
            }
        } catch (SQLException e) {
            event.getChannel().sendMessage(e.getMessage()).queue();
        }
    }
}