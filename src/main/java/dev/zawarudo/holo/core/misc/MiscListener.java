package dev.zawarudo.holo.core.misc;

import dev.zawarudo.holo.modules.emotes.EmoteManager;
import dev.zawarudo.holo.utils.Emote;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Class that defines miscellaneous behavior of the bot as response to certain events.
 */
public class MiscListener extends ListenerAdapter {

    private final EmoteManager emoteManager;
    private final Set<Long> existingEmotes;

    private static final Logger LOGGER = LoggerFactory.getLogger(MiscListener.class);

    public MiscListener(EmoteManager emoteManager) {
        this.emoteManager = emoteManager;

        try {
            existingEmotes = emoteManager.getEmoteIds();
        } catch (SQLException ex) {
            throw new IllegalStateException("Something went wrong while fetching the emote ids from the database.");
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        // Ignore default emojis
        if (event.getEmoji().getType() == Emoji.Type.UNICODE) {
            return;
        }
        storeNewEmotesInDatabase(event.getEmoji().asCustom());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        List<CustomEmoji> messageEmotes = event.getMessage().getMentions().getCustomEmojis();
        if (!messageEmotes.isEmpty()) {
            storeNewEmotesInDatabase(messageEmotes.toArray(new CustomEmoji[0]));
        }

        String content = event.getMessage().getContentRaw().toLowerCase(Locale.UK);

        // Add ❤ as reaction
        if (content.contains(":heart:") || content.contains("<3") || content.contains("❤")) {
            addReaction(event.getMessage(), Emote.HEART);
        }

        // Add 💀 as reaction
        if (Arrays.stream(content.split(" ")).anyMatch(s -> s.contains("forgor"))) {
            addReaction(event.getMessage(), Emote.SKULL);
        }
    }

    private void addReaction(@NotNull Message msg, @NotNull Emote emote) {
        msg.addReaction(emote.getAsEmoji()).queue(
            s -> LOGGER.info("REACTION: Reacted with {}.", emote.getAsText()),
            err -> LOGGER.warn("REACTION: Can't react with {} because I have been blocked by {} (ID: {}).",
                emote.getAsText(),
                msg.getAuthor().getName(),
                msg.getAuthor().getId()));
    }

    private void storeNewEmotesInDatabase(CustomEmoji... emotes) {
        List<CustomEmoji> newEmotes = Arrays.stream(emotes)
            .filter(e -> !existingEmotes.contains(e.getIdLong()))
            .toList();

        if (newEmotes.isEmpty()) return;

        try {
            emoteManager.insertEmotes(newEmotes.toArray(CustomEmoji[]::new));
            existingEmotes.addAll(newEmotes.stream().map(ISnowflake::getIdLong).toList());

            if (LOGGER.isInfoEnabled()) {
                String emoteInfo = newEmotes.stream()
                    .map(e -> "%s (%d)".formatted(e.getName(), e.getIdLong()))
                    .toList()
                    .toString();

                LOGGER.info("Successfully stored {} new emotes: {}", newEmotes.size(), emoteInfo);
            }
        } catch (SQLException ex) {
            LOGGER.error("Something went wrong while storing new emotes.", ex);
        }
    }
}
