package dev.zawarudo.holo.core.command;

import dev.zawarudo.holo.core.misc.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class MessageReply implements CommandContext.Reply {

    private static final int ERROR_DELETE_AFTER_SECONDS = 30;

    private final @NotNull MessageReceivedEvent event;
    private final @NotNull Message replyTo;

    public MessageReply(@NotNull MessageReceivedEvent event) {
        this(event, event.getMessage());
    }

    public MessageReply(@NotNull MessageReceivedEvent event, @NotNull Message replyTo) {
        this.event = event;
        this.replyTo = replyTo;
    }

    @Override
    public void typing() {
        event.getChannel().sendTyping().queue();
    }

    @Override
    public void text(@NotNull String content) {
        replyTo.reply(content).queue();
    }

    @Override
    public void embed(@NotNull EmbedBuilder embed) {
        replyTo.replyEmbeds(embed.build()).queue();
    }

    @Override
    public void embed(@NotNull MessageEmbed embed, int duration, TimeUnit unit) {
        replyTo.replyEmbeds(embed).queue(sent ->
            sent.delete().queueAfter(duration, unit, __ -> {
            }, __ -> {
            })
        );
    }

    @Override
    public void embedAndDeleteInvoke(@NotNull CommandContext ctx, @NotNull MessageEmbed embed, int duration, TimeUnit unit) {
        embed(embed, duration, unit);
        ctx.message().ifPresent(msg ->
            msg.delete().queueAfter(duration, unit, null, ignored -> {
            }));
    }

    @Override
    public void errorEmbed(@NotNull String content) {
        MessageEmbed embed = buildErrorEmbed(content);
        embed(embed, ERROR_DELETE_AFTER_SECONDS, TimeUnit.SECONDS);
    }

    public @NotNull Message replyTo() {
        return replyTo;
    }

    private @NotNull MessageEmbed buildErrorEmbed(@NotNull String content) {
        return new EmbedBuilder()
            .setTitle("Error")
            .setDescription(content)
            .setColor(EmbedColor.ERROR.getColor())
            .build();
    }
}
