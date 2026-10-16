package dev.zawarudo.holo.modules.pokemon;

import com.google.common.util.concurrent.MoreExecutors;
import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.zawarudo.holo.modules.pokemon.FakeDiscord.SELF;
import static dev.zawarudo.holo.modules.pokemon.FakeDiscord.fake;
import static dev.zawarudo.holo.modules.pokemon.SpawnFixture.CHANNEL_ID;
import static dev.zawarudo.holo.modules.pokemon.SpawnFixture.GUILD_ID;
import static org.junit.jupiter.api.Assertions.*;

class PokemonCatchListenerTest {

    private static final long ASH = 1;
    private static final long MISTY = 2;

    record Reply(String text, boolean ephemeral) {
    }

    @TempDir
    Path dir;

    private SpawnFixture f;
    private PokemonCatchListener listener;
    private final List<Reply> replies = new ArrayList<>();
    private Modal openedModal;
    private boolean editDeferred;

    @BeforeEach
    void setUp() throws Exception {
        f = new SpawnFixture(dir);
        f.start();
        f.manager.addChannel(f.channel);
        listener = new PokemonCatchListener(f.manager, MoreExecutors.newDirectExecutorService());
    }

    @Test
    void catchButtonOpensFormAndRightNameCatches() throws Exception {
        long spawn = f.wild().messageId();

        listener.onButtonInteraction(press("pokemon:catch", spawn, ASH));

        assertEquals("Who's that Pokémon?", openedModal.getTitle());
        listener.onModalInteraction(submit(openedModal, "Glurak", ASH));

        assertTrue(editDeferred);
        assertTrue(replies.isEmpty(), "no private reply on a catch");
        assertEquals("The wild Charizard has been caught!", f.discord.message(spawn).embed.getTitle());
        assertEquals(1, f.dao.findPoints(GUILD_ID, ASH));
    }

    @Test
    void wrongGuessesAreAnsweredPrivately() {
        long spawn = f.wild().messageId();
        listener.onButtonInteraction(press("pokemon:catch", spawn, ASH));

        listener.onModalInteraction(submit(openedModal, " Pikachu ", ASH));
        listener.onModalInteraction(submit(openedModal, "Raichu", ASH));
        listener.onModalInteraction(submit(openedModal, "**Mew**", ASH));

        assertEquals(List.of(
            new Reply("Not quite right!\n\nYour guess: **Pikachu**\n\n2 guesses left.", true),
            new Reply("Not quite right!\n\nYour guess: **Raichu**\n\n1 guess left.", true),
            new Reply("Not quite right!\n\nYour guess: **\\*\\*Mew\\*\\***\n\nThat was your last guess for this Pokémon.", true)
        ), replies);
        assertFalse(f.discord.message(spawn).edited, "nothing shown in the channel");
    }

    @Test
    void playerOutOfGuessesCantOpenFormOrSubmitOne() throws Exception {
        long spawn = f.wild().messageId();
        listener.onButtonInteraction(press("pokemon:catch", spawn, ASH));
        Modal earlierForm = openedModal;
        for (int i = 0; i < 3; i++) {
            listener.onModalInteraction(submit(earlierForm, "Pikachu", ASH));
        }
        replies.clear();
        openedModal = null;

        listener.onButtonInteraction(press("pokemon:catch", spawn, ASH));
        listener.onModalInteraction(submit(earlierForm, "Charizard", ASH));

        assertNull(openedModal);
        assertEquals(List.of(
            new Reply("You're out of guesses for this Pokémon.", true),
            new Reply("You're out of guesses for this Pokémon.", true)
        ), replies);
        assertTrue(f.dao.findCollection(ASH).isEmpty());
    }

    @Test
    void caughtPokemonIsGoneForOthers() throws Exception {
        long spawn = f.wild().messageId();
        listener.onButtonInteraction(press("pokemon:catch", spawn, ASH));
        f.catchAs(MISTY, "Misty", "Charizard");

        listener.onModalInteraction(submit(openedModal, "Charizard", ASH));
        listener.onButtonInteraction(press("pokemon:catch", spawn, ASH));

        assertEquals(List.of(
            new Reply("This Pokémon is no longer here.", true),
            new Reply("This Pokémon is no longer here.", true)
        ), replies);
        assertTrue(f.dao.findCollection(ASH).isEmpty());
    }

    @Test
    void disabledModuleRepliesPrivately() {
        f.enabled = false;

        listener.onButtonInteraction(press("pokemon:catch", f.wild().messageId(), ASH));

        assertNull(openedModal);
        assertEquals(List.of(new Reply("Pokémon are disabled on this server.", true)), replies);
    }

    @Test
    void otherButtonsAndFormsAreIgnored() {
        long spawn = f.wild().messageId();

        listener.onButtonInteraction(press("other:button", spawn, ASH));
        Modal otherForm = fake(Modal.class, Map.of("getId", _ -> "other:form", "getComponents", _ -> List.of()));
        listener.onModalInteraction(submit(otherForm, "Charizard", ASH));

        assertNull(openedModal);
        assertTrue(replies.isEmpty());
        assertFalse(editDeferred);
        assertEquals(spawn, f.wild().messageId());
    }

    private ButtonInteractionEvent press(String buttonId, long messageId, long userId) {
        ButtonInteraction interaction = fake(ButtonInteraction.class, Map.of(
            "getComponentId", _ -> buttonId,
            "getGuild", _ -> f.discord.guild,
            "getChannelIdLong", _ -> CHANNEL_ID,
            "getMessageIdLong", _ -> messageId,
            "getUser", _ -> FakeDiscord.user(userId, "Ash"),
            "deferReply", _ -> replyAction(),
            "replyModal", a -> {
                openedModal = (Modal) a[0];
                return fake(ModalCallbackAction.class, Map.of());
            }
        ));
        return new ButtonInteractionEvent(f.discord.jda, 0, interaction);
    }

    /** Submits the form with the guess typed into its first text input. */
    private ModalInteractionEvent submit(Modal form, String guess, long userId) {
        String inputId = form.getComponents().isEmpty() ? "name"
            : form.getComponents().getFirst().asLabel().getChild().asTextInput().getCustomId();
        DataObject input = DataObject.empty()
            .put("id", 1)
            .put("custom_id", inputId)
            .put("type", Component.Type.TEXT_INPUT.getKey())
            .put("value", guess);
        ModalInteraction interaction = fake(ModalInteraction.class, Map.of(
            "getModalId", _ -> form.getId(),
            "getGuild", _ -> f.discord.guild,
            "getChannelIdLong", _ -> CHANNEL_ID,
            "getUser", _ -> FakeDiscord.user(userId, "Ash"),
            "getValues", _ -> List.of(new ModalMapping(null, DataObject.empty(), input)),
            "deferReply", _ -> replyAction(),
            "deferEdit", _ -> fake(MessageEditCallbackAction.class, Map.of("queue", _ -> editDeferred = true))
        ));
        return new ModalInteractionEvent(f.discord.jda, 0, interaction);
    }

    private ReplyCallbackAction replyAction() {
        String[] text = new String[1];
        boolean[] ephemeral = new boolean[1];
        return fake(ReplyCallbackAction.class, Map.of(
            "setContent", a -> {
                text[0] = (String) a[0];
                return SELF;
            },
            "setEphemeral", a -> {
                ephemeral[0] = (boolean) a[0];
                return SELF;
            },
            "queue", _ -> replies.add(new Reply(text[0], ephemeral[0]))
        ));
    }
}
