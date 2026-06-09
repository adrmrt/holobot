package dev.zawarudo.holo.core.command;

import dev.zawarudo.holo.core.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Immutable snapshot of everything a command needs to handle one invocation.
 *
 * <p>Constructed before {@link ExecutableCommand#execute(CommandContext)} is called. Commands
 * must not hold references to this object beyond the scope of that call.
 */
public final class CommandContext {

    private final @NotNull String commandName;
    private final @NotNull String invokedAs;
    private final @NotNull List<String> args;

    private final @NotNull Invocation invocation;
    private final @NotNull Reply reply;

    private final boolean botOwner;
    private final boolean guildAdmin;

    private final @Nullable String prefix;
    private final @Nullable GuildConfig guildConfig;

    public CommandContext(
        @NotNull String commandName,
        @NotNull String invokedAs,
        @NotNull List<String> args,
        @NotNull Invocation invocation,
        @NotNull Reply reply,
        boolean botOwner,
        boolean guildAdmin,
        @Nullable String prefix,
        @Nullable GuildConfig guildConfig
    ) {
        this.commandName = requireNonBlank(commandName, "commandName");
        this.invokedAs = requireNonBlank(invokedAs, "invokedAs");
        this.args = List.copyOf(Objects.requireNonNull(args, "args"));
        this.invocation = Objects.requireNonNull(invocation, "invocation");
        this.reply = Objects.requireNonNull(reply);
        this.botOwner = botOwner;
        this.guildAdmin = guildAdmin;
        this.prefix = prefix;
        this.guildConfig = guildConfig;
    }

    /** Returns the canonical name of the command being executed. */
    @NotNull
    public String commandName() {
        return commandName;
    }

    /** Returns the alias or name the user actually typed to invoke the command. */
    @NotNull
    public String invokedAs() {
        return invokedAs;
    }

    /** Returns an immutable, ordered list of whitespace-split arguments (excluding the command token). */
    @NotNull
    public List<String> args() {
        return args;
    }

    /** Returns the {@link Invocation} giving access to the originating message and its author. */
    @NotNull
    public Invocation invocation() {
        return invocation;
    }

    /** Returns the {@link Reply} used to send responses for this invocation. */
    @NotNull
    public Reply reply() {
        return reply;
    }

    /** Returns {@code true} if the invoking user is the bot owner. */
    public boolean isBotOwner() {
        return botOwner;
    }

    /** Returns {@code true} if the invoking user has administrator permissions in the guild. */
    public boolean isGuildAdmin() {
        return guildAdmin;
    }

    /** Returns the prefix used in this guild, or empty if invoked without a prefix (e.g. slash commands). */
    public @NotNull Optional<String> prefix() {
        return Optional.ofNullable(prefix);
    }

    /** Returns the guild-specific configuration, or empty when invoked outside a guild. */
    public @NotNull Optional<GuildConfig> guildConfig() {
        return Optional.ofNullable(guildConfig);
    }

    /** Returns the source this invocation originated from (message or slash command). */
    public @NotNull CommandSource source() {
        return invocation.source();
    }

    /** Returns {@code true} if the command was invoked inside a guild. */
    public boolean inGuild() {
        return invocation.inGuild();
    }

    /** Returns the user who invoked the command. */
    public @NotNull User user() {
        return invocation.user();
    }

    /**
     * Returns the invoking user as a guild {@link Member}, or empty when invoked in a DM.
     */
    public @NotNull Optional<Member> member() {
        return Optional.ofNullable(invocation.member());
    }

    /**
     * Returns the guild in which the command was invoked, or empty when invoked in a DM.
     */
    public @NotNull Optional<Guild> guild() {
        return Optional.ofNullable(invocation.guild());
    }

    /** Returns the channel in which the command was invoked. */
    public @NotNull MessageChannelUnion channel() {
        return invocation.channel();
    }

    /** Returns the {@link JDA} instance. */
    public @NotNull JDA jda() {
        return invocation.channel().getJDA();
    }

    /**
     * Returns the originating {@link Message}, or empty for interaction-based invocations
     * that have no associated message.
     */
    public @NotNull Optional<Message> message() {
        return Optional.ofNullable(invocation.message());
    }

    /** Returns the number of arguments provided. */
    public int argCount() {
        return args.size();
    }

    /** Returns {@code true} if at least one argument was provided. */
    public boolean hasArgs() {
        return !args.isEmpty();
    }

    /**
     * Returns the argument at {@code index}, or empty if the index is out of bounds.
     *
     * @param index zero-based argument index
     */
    public @NotNull Optional<String> arg(int index) {
        return (index >= 0 && index < args.size()) ? Optional.of(args.get(index)) : Optional.empty();
    }

    /** Returns all arguments joined by a single space. */
    public @NotNull String argString() {
        return String.join(" ", args).trim();
    }

    /**
     * Provides access to the raw Discord objects of the invocation.
     */
    public interface Invocation {

        /** Returns the source of the invocation. */
        CommandSource source();

        /** Returns the user who triggered the command. */
        User user();

        /** Returns the guild member, or {@code null} if invoked in a DM. */
        @Nullable Member member();

        /** Returns {@code true} if the invocation originated inside a guild. */
        boolean inGuild();

        /** Returns the guild, or {@code null} if invoked in a DM. */
        @Nullable Guild guild();

        /** Returns the channel the command was sent in. */
        MessageChannelUnion channel();

        /** Returns the triggering message, or {@code null} for interaction-only invocations. */
        @Nullable Message message();

        /** Returns roles mentioned in the invocation message. */
        @NotNull List<Role> mentionedRoles();

        /** Returns members mentioned in the invocation message. */
        @NotNull List<Member> mentionedMembers();

        /**
         * Deletes the message that triggered the command if the bot has permission to do so.
         * Silently no-ops otherwise.
         */
        void deleteInvokeIfPossible();
    }

    /**
     * Represents the origin of a command invocation.
     */
    public enum CommandSource {
        /** Invoked via a chat message. */
        MESSAGE,
        /** Invoked via a slash command interaction. */
        SLASH
    }

    /**
     * Sends responses on behalf of a command invocation.
     *
     * <p>Implementations handle the difference between message-based and
     * interaction-based replies transparently.
     */
    public interface Reply {

        /** Sends a typing indicator to the channel. */
        void typing();

        /** Sends a plain-text message. */
        void text(@NotNull String content);

        /** Sends an embed. */
        void embed(@NotNull EmbedBuilder embed);

        /**
         * Sends an embed and deletes it after the given duration.
         *
         * @param embed    the embed to send
         * @param duration how long to wait before deletion
         * @param unit     the time unit for {@code duration}
         */
        void embed(@NotNull MessageEmbed embed, int duration, TimeUnit unit);

        /**
         * Sends an embed and deletes both the reply and the invoking message after the given duration.
         *
         * @param ctx      the current context (used to delete the invoke message)
         * @param embed    the embed to send
         * @param duration how long to wait before deletion
         * @param unit     the time unit for {@code duration}
         */
        void embedAndDeleteInvoke(@NotNull CommandContext ctx, @NotNull MessageEmbed embed, int duration, TimeUnit unit);

        /**
         * Sends an ephemeral text message. Falls back to a regular message in contexts
         * that do not support ephemeral replies.
         */
        default void ephemeralText(@NotNull String content) {
            text(content);
        }

        /** Sends a pre-styled error embed containing the given message. */
        void errorEmbed(@NotNull String content);
    }

    private static String requireNonBlank(String s, String name) {
        Objects.requireNonNull(s, name);
        if (s.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return s;
    }
}
