package dev.zawarudo.holo.core.command;

import dev.zawarudo.holo.core.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Immutable per-invocation context for commands.
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

    @NotNull
    public String commandName() {
        return commandName;
    }

    @NotNull
    public String invokedAs() {
        return invokedAs;
    }

    @NotNull
    public List<String> args() {
        return args;
    }

    @NotNull
    public Invocation invocation() {
        return invocation;
    }

    @NotNull
    public Reply reply() {
        return reply;
    }

    public boolean isBotOwner() {
        return botOwner;
    }

    public boolean isGuildAdmin() {
        return guildAdmin;
    }

    public @NotNull Optional<String> prefix() {
        return Optional.ofNullable(prefix);
    }

    public @NotNull Optional<GuildConfig> guildConfig() {
        return Optional.ofNullable(guildConfig);
    }

    public @NotNull CommandSource source() {
        return invocation.source();
    }

    public boolean inGuild() {
        return invocation.inGuild();
    }

    public @NotNull User user() {
        return invocation.user();
    }

    public @NotNull Optional<Member> member() {
        return Optional.ofNullable(invocation.member());
    }

    public @NotNull Optional<Guild> guild() {
        return Optional.ofNullable(invocation.guild());
    }

    public @NotNull MessageChannelUnion channel() {
        return invocation.channel();
    }

    public @NotNull Optional<Message> message() {
        return Optional.ofNullable(invocation.message());
    }

    public int argCount() {
        return args.size();
    }

    public boolean hasArgs() {
        return !args.isEmpty();
    }

    public @NotNull Optional<String> arg(int index) {
        return (index >= 0 && index < args.size()) ? Optional.of(args.get(index)) : Optional.empty();
    }

    public @NotNull String argString() {
        return String.join(" ", args).trim();
    }

    public interface Invocation {

        CommandSource source();

        User user();

        @Nullable Member member();

        boolean inGuild();

        @Nullable Guild guild();

        MessageChannelUnion channel();

        @Nullable Message message();

        @NotNull List<Role> mentionedRoles();

        @NotNull List<Member> mentionedMembers();

        void deleteInvokeIfPossible();
    }

    public enum CommandSource {
        MESSAGE,
        SLASH
    }

    public interface Reply {
        void typing();

        void text(@NotNull String content);

        void embed(@NotNull EmbedBuilder embed);

        void embed(@NotNull MessageEmbed embed, int duration, TimeUnit unit);

        void embedAndDeleteInvoke(@NotNull CommandContext ctx, @NotNull MessageEmbed embed, int duration, TimeUnit unit);

        default void ephemeralText(@NotNull String content) {
            text(content);
        }

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
