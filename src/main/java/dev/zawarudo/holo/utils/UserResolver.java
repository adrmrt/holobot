package dev.zawarudo.holo.utils;

import dev.zawarudo.holo.core.command.CommandContext;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.Optional;

/**
 * Resolves JDA {@link User} and {@link Member} instances from a command invocation.
 */
public final class UserResolver {

    /**
     * Returns the target user for a command. If an argument is present it is treated as a user
     * ID or mention ({@code <@ID>}). Otherwise, falls back to the invoker when absent or unresolvable.
     */
    public Optional<User> resolveUser(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            return Optional.of(ctx.user());
        }
        String userId = ctx.args().getFirst().replaceAll("\\D", "");
        if (!userId.isEmpty()) {
            try {
                long id = Long.parseLong(userId);
                return Optional.ofNullable(ctx.jda().getUserById(id));
            } catch (NumberFormatException ignored) {
            }
        }
        return Optional.of(ctx.user());
    }

    /**
     * Looks up the given user as a member of the guild in the context. Returns empty if the
     * command was invoked outside a guild or the user is not a member of it.
     */
    public Optional<Member> resolveGuildMember(User user, CommandContext ctx) {
        return ctx.guild().flatMap(guild -> {
            try {
                return Optional.of(guild.retrieveMember(user).complete());
            } catch (ErrorResponseException ex) {
                return Optional.empty();
            }
        });
    }
}
