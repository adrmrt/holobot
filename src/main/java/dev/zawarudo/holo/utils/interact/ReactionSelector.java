package dev.zawarudo.holo.utils.interact;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import dev.zawarudo.holo.utils.Emote;
import dev.zawarudo.holo.utils.HoloUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class ReactionSelector<T> {

    public interface ListRenderer<T> {
        @NotNull MessageEmbed render(@NotNull List<T> list);
    }

    public interface SelectionHandler<T> {
        void onSelect(@NotNull MessageReactionAddEvent evt, @NotNull T selected, int index);
    }

    public interface TimeoutHandler {
        void onTimeout(@NotNull Message msg);
    }

    private final EventWaiter waiter;
    private final ListRenderer<T> listRenderer;

    private final long timeout;
    private final TimeUnit timeoutUnit;

    private final boolean deleteOnSelect;
    private final boolean deleteOnTimeout;

    public ReactionSelector(@NotNull EventWaiter waiter, @NotNull ListRenderer<T> listRenderer) {
        this(waiter, listRenderer, 5, TimeUnit.MINUTES, true, true);
    }

    public ReactionSelector(
        @NotNull EventWaiter waiter,
        @NotNull ListRenderer<T> listRenderer,
        long timeout,
        @NotNull TimeUnit timeoutUnit,
        boolean deleteOnSelect,
        boolean deleteOnTimeout
    ) {
        this.waiter = waiter;
        this.listRenderer = listRenderer;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.deleteOnSelect = deleteOnSelect;
        this.deleteOnTimeout = deleteOnTimeout;
    }

    public void start(
        @NotNull Message commandMessage,
        @NotNull User caller,
        @NotNull List<T> items,
        @NotNull SelectionHandler<T> onSelect
    ) {
        start(commandMessage, caller, items, onSelect, null);
    }

    public void start(
        @NotNull Message commandMessage,
        @NotNull User caller,
        @NotNull List<T> items,
        @NotNull SelectionHandler<T> onSelect,
        @Nullable TimeoutHandler onTimeout
    ) {
        Objects.requireNonNull(commandMessage, "commandMessage");
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(onSelect, "onSelect");

        if (items.isEmpty()) return;

        // Limit to the amount of available number emotes
        List<Emote> numbers = HoloUtils.getNumbers();
        int max = Math.min(items.size(), numbers.size());

        List<T> shown = items.subList(0, max);

        MessageEmbed listEmbed = listRenderer.render(shown);

        commandMessage.replyEmbeds(listEmbed).queue(msg -> {
            HoloUtils.addReactions(msg, max);

            await(msg, caller, shown, numbers, onSelect, onTimeout);
        }, err -> {
            // ignore
        });
    }

    private void await(
        @NotNull Message msg,
        @NotNull User caller,
        @NotNull List<T> items,
        @NotNull List<Emote> numbers,
        @NotNull SelectionHandler<T> onSelect,
        @Nullable TimeoutHandler onTimeout
    ) {
        waiter.waitForEvent(
            MessageReactionAddEvent.class,
            evt -> isValid(evt, msg, caller, items, numbers),
            evt -> handle(evt, msg, items, numbers, onSelect),
            timeout,
            timeoutUnit,
            () -> handleTimeout(msg, onTimeout)
        );
    }

    private boolean isValid(
        @NotNull MessageReactionAddEvent evt,
        @NotNull Message msg,
        @NotNull User caller,
        @NotNull List<T> items,
        @NotNull List<Emote> numbers
    ) {
        if (evt.getMessageIdLong() != msg.getIdLong()) return false;

        // Ignore bots
        if (evt.getUser() == null || evt.getUser().isBot()) return false;

        // Only the command caller may select
        if (!evt.getUser().equals(caller)) return false;

        // Must match one of the numbered reactions
        for (int i = 0; i < items.size(); i++) {
            if (evt.getReaction().getEmoji().equals(numbers.get(i).getAsEmoji())) {
                return true;
            }
        }
        return false;
    }

    private void handle(
        @NotNull MessageReactionAddEvent evt,
        @NotNull Message msg,
        @NotNull List<T> items,
        @NotNull List<Emote> numbers,
        @NotNull SelectionHandler<T> onSelect
    ) {
        int index = indexFromReaction(evt, items.size(), numbers);
        if (index < 0) {
            // At this bot, user has been validated
            Objects.requireNonNull(evt.getUser(), "user");

            // Should not happen because of isValid check
            await(msg, evt.getUser(), items, numbers, onSelect, null);
            return;
        }

        if (deleteOnSelect) {
            msg.delete().queue(null, ignored -> {
            });
        }

        onSelect.onSelect(evt, items.get(index), index);
    }

    private void handleTimeout(@NotNull Message msg, @Nullable TimeoutHandler onTimeout) {
        if (onTimeout != null) {
            onTimeout.onTimeout(msg);
            return;
        }
        if (deleteOnTimeout) {
            msg.delete().queue(null, ignored -> {
            });
        }
    }

    private int indexFromReaction(
        @NotNull MessageReactionAddEvent evt,
        int size,
        @NotNull List<Emote> numbers
    ) {
        for (int i = 0; i < size; i++) {
            if (evt.getReaction().getEmoji().equals(numbers.get(i).getAsEmoji())) {
                return i;
            }
        }
        return -1;
    }

    public static <T> MessageEmbed defaultNumberedListEmbed(
        @NotNull String title,
        @NotNull List<T> items,
        @NotNull Function<T, String> lineFormatter,
        Color color
    ) {
        return defaultNumberedListEmbed(title, items, lineFormatter, color, null, null, null);
    }

    public static <T> MessageEmbed defaultNumberedListEmbed(
        @NotNull String title,
        @NotNull List<T> items,
        @NotNull Function<T, String> lineFormatter,
        Color color,
        @Nullable String authorName,
        @Nullable String authorUrl,
        @Nullable String authorIconUrl
    ) {
        List<Emote> numbers = HoloUtils.getNumbers();
        int max = Math.min(items.size(), numbers.size());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(numbers.get(i).getAsEmoji().getFormatted())
                .append(" ")
                .append(lineFormatter.apply(items.get(i)))
                .append("\n");
        }

        EmbedBuilder b = new EmbedBuilder();
        b.setTitle(title);
        b.setDescription(sb + "\nTo select one item, please react with the corresponding number.");
        b.setColor(color);
        if (authorName != null) {
            b.setAuthor(authorName, authorUrl, authorIconUrl);
        }
        return b.build();
    }
}
