package dev.zawarudo.holo.core.command;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MessageInvocation implements CommandContext.Invocation {

    private final @NotNull MessageReceivedEvent event;

    public MessageInvocation(@NotNull MessageReceivedEvent event) {
        this.event = event;
    }

    @Override
    public CommandContext.CommandSource source() {
        return CommandContext.CommandSource.MESSAGE;
    }

    @Override
    public User user() {
        return event.getAuthor();
    }

    @Override
    public @Nullable Member member() {
        return event.getMember();
    }

    @Override
    public boolean inGuild() {
        return event.isFromGuild();
    }

    @Override
    public @Nullable Guild guild() {
        return event.isFromGuild() ? event.getGuild() : null;
    }

    @Override
    public MessageChannelUnion channel() {
        return event.getChannel();
    }

    @Override
    public @NotNull Message message() {
        return event.getMessage();
    }

    @Override
    public @NotNull List<Role> mentionedRoles() {
        if (!inGuild()) {
            return List.of();
        }

        List<Role> roles = event.getMessage()
            .getMentions()
            .getRoles();

        return roles.isEmpty() ? List.of() : List.copyOf(roles);
    }

    @Override
    public @NotNull List<Member> mentionedMembers() {
        if (!inGuild()) {
            return List.of();
        }

        List<Member> members = event.getMessage()
            .getMentions()
            .getMembers();

        return members.isEmpty() ? List.of() : List.copyOf(members);
    }

    @Override
    public void deleteInvokeIfPossible() {
        // DMs cannot delete messages
        if (!inGuild()) {
            return;
        }

        boolean canDelete = PermissionUtil.checkPermission(
            event.getGuildChannel().getPermissionContainer(),
            event.getGuild().getSelfMember(),
            Permission.MESSAGE_MANAGE
        );

        if (!canDelete) return;

        event.getMessage()
            .delete()
            .queue(
                success -> {
                },
                failure -> {
                }
            );
    }
}
