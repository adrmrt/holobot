package dev.zawarudo.holo.utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for sending embeds that should be removed again after a delay.
 */
public final class EmbedUtils {

    private EmbedUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sends an embed to the given channel and deletes it again after the given delay.
     */
    public static void sendTimed(@NotNull MessageChannelUnion channel, @NotNull MessageEmbed embed, long delay, @NotNull TimeUnit unit) {
        deleteAfter(channel.sendMessageEmbeds(embed), delay, unit);
    }

    /**
     * Queues the given action and deletes the resulting message again after the given delay.
     */
    public static void deleteAfter(@NotNull RestAction<Message> action, long delay, @NotNull TimeUnit unit) {
        action.queue(msg -> msg.delete()
            .queueAfter(delay, unit, null, _ -> {
                // Ignore if message is already deleted
            })
        );
    }
}
