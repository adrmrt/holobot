package dev.zawarudo.holo.core.misc;

import dev.zawarudo.holo.core.command.CommandContext;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;

/**
 * Represents a submission from a user to the bot owner. This can either
 * be a bug report or a suggestion.
 */
public class Submission {

    public final String type;
    public final String date;
    public final Guild guild;
    public final Channel channel;
    public final User author;
    public final String message;

    public Submission(String type, CommandContext ctx, String message) {
        this.type = type;
        this.date = ctx.message().map(m -> m.getTimeCreated().toString()).orElse("");
        this.guild = ctx.guild().orElse(null);
        this.channel = ctx.channel();
        this.author = ctx.user();
        this.message = message;
    }
}
