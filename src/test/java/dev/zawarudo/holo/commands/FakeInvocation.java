package dev.zawarudo.holo.commands;

import dev.zawarudo.holo.core.command.CommandContext;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * No-op Invocation for use in command unit tests. Returns null for all JDA objects.
 * Override individual methods when a test needs specific behaviour.
 */
public class FakeInvocation implements CommandContext.Invocation {

    @Override
    public CommandContext.CommandSource source() {
        return CommandContext.CommandSource.MESSAGE;
    }

    @Override
    public User user() {
        return null;
    }

    @Override
    public @Nullable Member member() {
        return null;
    }

    @Override
    public boolean inGuild() {
        return false;
    }

    @Override
    public @Nullable Guild guild() {
        return null;
    }

    @Override
    public MessageChannelUnion channel() {
        return null;
    }

    @Override
    public @Nullable Message message() {
        return null;
    }

    @Override
    public @NotNull List<Role> mentionedRoles() {
        return List.of();
    }

    @Override
    public @NotNull List<Member> mentionedMembers() {
        return List.of();
    }

    @Override
    public void deleteInvokeIfPossible() {
    }
}
