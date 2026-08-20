package dev.zawarudo.holo.commands;

import dev.zawarudo.holo.core.command.CommandContext;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Capturing Reply for use in command unit tests. Records the last call made to
 * each reply method so tests can assert on it.
 */
public class CapturingReply implements CommandContext.Reply {

    public String lastText;
    public String lastError;

    @Override
    public void typing() {
    }

    @Override
    public void text(@NotNull String content) {
        lastText = content;
    }

    @Override
    public void embed(@NotNull EmbedBuilder embed) {
    }

    @Override
    public void embed(@NotNull MessageEmbed embed, int duration, TimeUnit unit) {
    }

    @Override
    public void embedAndDeleteInvoke(@NotNull CommandContext ctx, @NotNull MessageEmbed embed, int duration, TimeUnit unit) {
    }

    @Override
    public void errorEmbed(@NotNull String content) {
        lastError = content;
    }
}
