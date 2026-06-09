package dev.zawarudo.holo.core.command;

import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.core.GuildConfig;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Builds {@link CommandContext} instances for message-based commands.
 */
public final class CommandContextFactory {

    private final @NotNull Locale defaultLocale;

    public CommandContextFactory() {
        this(Locale.ENGLISH);
    }

    public CommandContextFactory(@NotNull Locale defaultLocale) {
        this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale");
    }

    public @NotNull CommandContext createForMessage(
        @NotNull MessageReceivedEvent event,
        @NotNull String commandName,
        @NotNull String invokedAs,
        @NotNull List<String> args,
        @Nullable String prefix
    ) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(args, "args");

        GuildConfig cfg = null;
        Locale locale = defaultLocale;

        if (event.isFromGuild()) {
            cfg = Bootstrap.holo.getGuildConfigManager().getOrCreate(event.getGuild());
        }

        boolean isOwner = event.getAuthor().getIdLong() == Bootstrap.holo.getConfig().getOwnerId();
        boolean isAdmin = isGuildAdmin(event.getMember());

        CommandContext.Invocation invocation = new MessageInvocation(event);
        CommandContext.Reply reply = new MessageReply(event);

        return new CommandContext(
            commandName,
            invokedAs,
            args,
            invocation,
            reply,
            isOwner,
            isAdmin,
            prefix,
            cfg
        );
    }

    private static boolean isGuildAdmin(@Nullable Member member) {
        if (member == null) return false;
        return member.isOwner();
    }
}
