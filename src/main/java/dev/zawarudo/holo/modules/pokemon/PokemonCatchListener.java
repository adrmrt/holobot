package dev.zawarudo.holo.modules.pokemon;

import dev.zawarudo.holo.modules.pokemon.PokemonSpawnManager.GuessResult;
import dev.zawarudo.holo.modules.pokemon.PokemonSpawnManager.WildPokemon;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Handles the Catch button on spawn messages and the guess form it opens. Every answer except a
 * successful catch is only visible to the guesser, so others can't learn from wrong guesses.
 */
public class PokemonCatchListener extends ListenerAdapter {

    /** Followed by the spawn message id, so a late guess can't hit the next Pokemon. */
    private static final String GUESS_MODAL_PREFIX = "pokemon:guess:";
    private static final String NAME_INPUT_ID = "name";

    private final PokemonSpawnManager spawnManager;
    private final ExecutorService io;

    public PokemonCatchListener(PokemonSpawnManager spawnManager, ExecutorService io) {
        this.spawnManager = spawnManager;
        this.io = io;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!PokemonSpawnManager.CATCH_BUTTON_ID.equals(event.getComponentId()) || event.getGuild() == null) {
            return;
        }
        if (!spawnManager.isEnabled(event.getGuild())) {
            reply(event, "Pokémon are disabled on this server.");
            return;
        }

        Optional<WildPokemon> current = spawnManager.getWild(event.getChannelIdLong())
            .filter(pokemon -> pokemon.messageId() == event.getMessageIdLong());
        if (current.isEmpty()) {
            reply(event, "This Pokémon is no longer here.");
            return;
        }
        if (spawnManager.guessesLeft(current.get(), event.getUser().getIdLong()) <= 0) {
            reply(event, "You're out of guesses for this Pokémon.");
            return;
        }

        TextInput name = TextInput.create(NAME_INPUT_ID, TextInputStyle.SHORT)
            .setPlaceholder("English or German name")
            .setMaxLength(50)
            .build();
        Modal modal = Modal.create(GUESS_MODAL_PREFIX + event.getMessageIdLong(), "Who's that Pokémon?")
            .addComponents(Label.of("Name", name))
            .build();
        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().startsWith(GUESS_MODAL_PREFIX) || event.getGuild() == null) {
            return;
        }
        long messageId = Long.parseLong(event.getModalId().substring(GUESS_MODAL_PREFIX.length()));
        ModalMapping value = event.getValue(NAME_INPUT_ID);
        String guess = value == null ? "" : value.getAsString();

        GuessResult result = spawnManager.guess(event.getChannelIdLong(), messageId, event.getUser().getIdLong(), guess);
        switch (result) {
            case GuessResult.Gone _ -> reply(event, "This Pokémon is no longer here.");
            case GuessResult.NoGuessesLeft _ -> reply(event, "You're out of guesses for this Pokémon.");
            case GuessResult.Wrong(int left) -> reply(event, wrongGuessText(guess, left));
            case GuessResult.Caught(WildPokemon caught) -> {
                event.deferEdit().queue();
                String catcher = event.getMember() != null ? event.getMember().getEffectiveName() : event.getUser().getName();
                io.execute(() -> spawnManager.completeCatch(caught, event.getUser(), catcher));
            }
        }
    }

    private static String wrongGuessText(String guess, int guessesLeft) {
        String left = switch (guessesLeft) {
            case 0 -> "That was your last guess for this Pokémon.";
            case 1 -> "1 guess left.";
            default -> guessesLeft + " guesses left.";
        };
        // Escaped so the guess can't break the bold formatting
        return "Not quite right!\n\nYour guess: **" + MarkdownSanitizer.escape(guess.strip()) + "**\n\n" + left;
    }

    private static void reply(IReplyCallback event, String text) {
        event.reply(text).setEphemeral(true).queue();
    }
}
