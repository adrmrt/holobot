package dev.zawarudo.holo.utils.interact;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import dev.zawarudo.holo.utils.Emote;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class ButtonPaginator<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ButtonPaginator.class);

    public interface PageRenderer<T> {
        /**
         * Render the embed for a page.
         */
        @NotNull
        MessageEmbed render(@NotNull T item, int index, int total);
    }

    private final String buttonPrefix;

    private static final String ID_PREV = "prev";
    private static final String ID_NEXT = "next";
    private static final String ID_EXIT = "exit";

    private final EventWaiter waiter;
    private final PageRenderer<T> renderer;

    private final long timeout;
    private final TimeUnit timeoutUnit;

    public ButtonPaginator(
        @NotNull EventWaiter waiter,
        @NotNull PageRenderer<T> renderer,
        @NotNull String buttonPrefix,
        long timeout,
        @NotNull TimeUnit timeoutUnit
    ) {
        this.waiter = Objects.requireNonNull(waiter);
        this.renderer = Objects.requireNonNull(renderer);
        this.buttonPrefix = Objects.requireNonNull(buttonPrefix);
        this.timeout = timeout;
        this.timeoutUnit = Objects.requireNonNull(timeoutUnit);
    }

    /**
     * Start a paginator by replying to an existing message.
     */
    public void start(@NotNull Message commandMessage, @NotNull User caller, @NotNull List<T> items) {
        if (items.isEmpty()) {
            return;
        }

        int index = 0;
        MessageEmbed embed;
        try {
            embed = renderer.render(items.get(index), index, items.size());
        } catch (Exception e) {
            LOG.error("ButtonPaginator renderer failed on page 0", e);
            return;
        }
        List<Button> buttons = buildButtons(index, items.size());

        commandMessage.replyEmbeds(embed)
            .addComponents(ActionRow.of(buttons))
            .queue(msg -> await(msg, caller, index, items), err ->
                LOG.error("ButtonPaginator failed to send initial message", err));
    }

    private void await(@NotNull Message msg, @NotNull User caller, int index, @NotNull List<T> items) {
        waiter.waitForEvent(
            ButtonInteractionEvent.class,
            evt -> isValid(evt, msg, caller),
            evt -> onButton(evt, msg, caller, index, items),
            timeout, timeoutUnit,
            () -> onTimeout(msg)
        );
    }

    private boolean isValid(@NotNull ButtonInteractionEvent evt, @NotNull Message msg, @NotNull User caller) {
        if (evt.getMessageIdLong() != msg.getIdLong()) return false;
        if (evt.getUser().isBot()) return false;

        String id = stripPrefix(evt.getButton().getCustomId());
        if (id == null) return false;

        boolean isKnown = ID_PREV.equals(id) || ID_NEXT.equals(id) || ID_EXIT.equals(id);
        if (!isKnown) return false;

        if (!evt.getUser().equals(caller)) {
            evt.reply("This command was not called by you!").setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    private void onButton(@NotNull ButtonInteractionEvent evt, @NotNull Message msg, @NotNull User caller, int index, @NotNull List<T> items) {
        String id = stripPrefix(evt.getButton().getCustomId());

        // Immediate delete
        if (ID_EXIT.equals(id)) {
            evt.deferEdit().queue(
                _ -> msg.delete().queue(),
                _ -> msg.delete().queue()
            );
            return;
        }

        int newIndex = index;
        if (ID_PREV.equals(id)) {
            newIndex = Math.max(0, index - 1);
        } else if (ID_NEXT.equals(id)) {
            newIndex = Math.min(items.size() - 1, index + 1);
        }

        MessageEmbed embed;
        try {
            embed = renderer.render(items.get(newIndex), newIndex, items.size());
        } catch (Exception e) {
            LOG.error("ButtonPaginator renderer failed on page {}", newIndex, e);
            await(msg, caller, index, items);
            return;
        }
        List<Button> buttons = buildButtons(newIndex, items.size());
        final int nextIndex = newIndex;

        // Acknowledge and edit
        evt.deferEdit().queue(
            hook -> hook.editOriginalEmbeds(embed)
                .setComponents(ActionRow.of(buttons))
                .queue(
                    ok -> await(msg, caller, nextIndex, items),
                    error -> await(msg, caller, nextIndex, items)
                ),
            error -> await(msg, caller, nextIndex, items));
    }

    private void onTimeout(@NotNull Message msg) {
        List<MessageEmbed> embeds = msg.getEmbeds();
        if (embeds.isEmpty()) {
            msg.editMessageComponents().queue(null, _ -> {
            });
            return;
        }
        // Remove footer with page info
        MessageEmbed stripped = new EmbedBuilder(embeds.getFirst()).setFooter(null).build();
        // Remove action row
        msg.editMessageEmbeds(stripped).setComponents().queue(null, _ -> {
        });
    }

    private List<Button> buildButtons(int index, int total) {
        List<Button> buttons = new ArrayList<>(3);

        buttons.add(makePrevButton(index == 0));
        buttons.add(makeExitButton());
        buttons.add(makeNextButton(index >= total - 1));

        return buttons;
    }

    private Button makePrevButton(boolean disabled) {
        return Button.primary(prefixedId(ID_PREV), Emote.ARROW_LEFT.getAsEmoji()).withDisabled(disabled);
    }

    private Button makeExitButton() {
        return Button.danger(prefixedId(ID_EXIT), Emote.TRASH_BIN.getAsEmoji());
    }

    private Button makeNextButton(boolean disabled) {
        return Button.primary(prefixedId(ID_NEXT), Emote.ARROW_RIGHT.getAsEmoji()).withDisabled(disabled);
    }

    private String prefixedId(String id) {
        return buttonPrefix + ":" + id;
    }

    private @Nullable String stripPrefix(@Nullable String customId) {
        String prefix = buttonPrefix + ":";
        if (customId == null || !customId.startsWith(prefix)) {
            return null;
        }
        return customId.substring(prefix.length());
    }

    /**
     * Small helper if you want a default footer like "Page X / Y".
     */
    public static MessageEmbed withPageFooter(@NotNull EmbedBuilder builder, int index, int total) {
        builder.setFooter(String.format("Page %d / %d", index + 1, total));
        return builder.build();
    }
}
