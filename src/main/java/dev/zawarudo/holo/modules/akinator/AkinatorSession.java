package dev.zawarudo.holo.modules.akinator;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.eu.zajc.akiwrapper.Akiwrapper;
import org.eu.zajc.akiwrapper.AkiwrapperBuilder;
import org.eu.zajc.akiwrapper.core.entities.Guess;
import org.eu.zajc.akiwrapper.core.entities.Query;
import org.eu.zajc.akiwrapper.core.entities.Question;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class AkinatorSession {

    public enum State {START, ASKING, GUESSING, FINISHED}

    // Button actions
    private static final String YES = "yes";
    private static final String NO = "no";
    private static final String DONT_KNOW = "dk";
    private static final String PROBABLY = "prob";
    private static final String PROBABLY_NOT = "pnot";
    private static final String UNDO = "undo";
    private static final String CONFIRM = "confirm";
    private static final String REJECT = "reject";
    private static final String STOP = "stop";
    private static final String START = "start";

    private final long userId;
    private final long channelId;
    private final long guildId;
    private final String id; // Session id for custom button ids

    private final EventWaiter waiter;
    private final AkinatorSessionManager manager;

    private final Akiwrapper aki;
    private final AkinatorEmbedRenderer renderer = new AkinatorEmbedRenderer();

    private volatile State state = State.START;
    private volatile boolean finished = false;

    private volatile Message message;
    private volatile Query currentQuery;

    private volatile Guess finalGuess;

    private long questionsAnswered = 0;

    public AkinatorSession(
            long userId,
            long channelId,
            long guildId,
            @NotNull EventWaiter waiter,
            @NotNull AkinatorSessionManager manager
    ) {
        this.userId = userId;
        this.channelId = channelId;
        this.guildId = guildId;

        this.waiter = waiter;
        this.manager = manager;

        this.id = UUID.randomUUID().toString().substring(0, 8);

        this.aki = new AkiwrapperBuilder()
                .setLanguage(Akiwrapper.Language.ENGLISH)
                .setTheme(Akiwrapper.Theme.CHARACTER)
                .setFilterProfanity(true)
                .build();
    }

    public long userId() {
        return userId;
    }

    public boolean isFinished() {
        return finished;
    }

    public State state() {
        return state;
    }

    public String sessionId() {
        return id;
    }

    public void start(@NotNull Message commandMsg) {
        state = State.START;

        var rendered = renderer.renderStart(); // new
        var rows = List.of(ActionRow.of(
                Button.primary(prefixed(START), "Start"),
                Button.danger(prefixed(STOP), "Cancel")
        ));

        commandMsg.replyEmbeds(rendered.embed())
                .addComponents(rows)
                .addFiles(resource(rendered.attachmentName()))
                .queue(msg -> {
                    this.message = msg;
                    awaitNext();
                }, err -> finish(false, "Failed to start Akinator (send message failed)."));
    }

    private void awaitNext() {
        if (finished || message == null) return;

        waiter.waitForEvent(
                ButtonInteractionEvent.class,
                this::isValid,
                this::onButton,
                2, TimeUnit.MINUTES,
                this::onTimeout
        );
    }

    private boolean isValid(ButtonInteractionEvent evt) {
        if (finished) return false;
        if (message == null) return false;
        if (evt.getMessageIdLong() != message.getIdLong()) return false;
        if (evt.getUser().isBot()) return false;

        if (evt.getUser().getIdLong() != userId) {
            evt.reply("This Akinator game is not yours.").setEphemeral(true).queue();
            return false;
        }

        String cid = evt.getButton().getCustomId();
        return cid != null && cid.startsWith(prefix());
    }

    private void onButton(ButtonInteractionEvent evt) {
        String action = stripPrefix(evt.getButton().getCustomId());
        if (action == null) {
            evt.deferEdit().queue();
            awaitNext();
            return;
        }

        // Always ack quickly
        evt.deferEdit().queue();

        try {
            // stop/cancel
            if (STOP.equals(action)) {
                finish(false, "Game cancelled.");
                editFinal(AkinatorEmbedRenderer.EndScreen.CANCEL);
                return;
            }

            if (State.START == state) {
                if (START.equals(action)) {
                    currentQuery = aki.getCurrentQuery();   // now fetch first query
                    state = State.ASKING;

                    var rendered = renderer.render(currentQuery, questionsAnswered);
                    var rows = toRows(buttonsFor(currentQuery));

                    message.editMessageEmbeds(rendered.embed())
                            .setComponents(rows)
                            .setFiles(resource(rendered.attachmentName()))
                            .queue(ok -> awaitNext(), err -> awaitNext());
                    return;
                }

                awaitNext();
                return;
            }

            if (currentQuery instanceof Question q) {
                if (UNDO.equals(action)) {
                    currentQuery = q.undoAnswer();
                    if (questionsAnswered > 0) questionsAnswered--;
                } else {
                    Akiwrapper.Answer ans = mapAnswer(action);
                    if (ans != null) {
                        currentQuery = q.answer(ans);
                        questionsAnswered++;
                    }
                }
            } else if (currentQuery instanceof Guess g) {
                state = State.GUESSING;

                if (CONFIRM.equals(action)) {
                    g.confirm();
                    finalGuess = g;
                    finish(true, "Akinator guessed correctly!");
                    editFinal(AkinatorEmbedRenderer.EndScreen.VICTORY);
                    return;
                } else if (REJECT.equals(action)) {
                    currentQuery = g.reject();
                }
            } else {
                // query == null => no more questions, player "wins"
                finish(false, "I ran out of questions — you win!");
                editFinal(AkinatorEmbedRenderer.EndScreen.DEFEAT);
                return;
            }

            // after update: if query becomes null, player wins
            if (currentQuery == null) {
                finish(false, "I ran out of questions — you win!");
                editFinal(AkinatorEmbedRenderer.EndScreen.DEFEAT);
                return;
            }

            var rendered = renderer.render(currentQuery, questionsAnswered);
            var rows = toRows(buttonsFor(currentQuery));

            message.editMessageEmbeds(rendered.embed())
                    .setComponents(rows)
                    .setFiles(resource(rendered.attachmentName()))
                    .queue(ok -> awaitNext(), err -> awaitNext());

        } catch (Exception ex) {
            finish(false, "Akinator error: " + ex.getMessage());
            editFinal(AkinatorEmbedRenderer.EndScreen.DEFEAT);
        }
    }

    private void onTimeout() {
        if (finished) return;
        finish(false, "Game timed out.");
        editFinal(AkinatorEmbedRenderer.EndScreen.TIMEOUT);
    }

    private void finish(boolean akinatorWon, String reason) {
        if (finished) return;
        finished = true;
        state = State.FINISHED;
        manager.removeSession(userId);
    }

    private void editFinal(AkinatorEmbedRenderer.EndScreen screen) {
        if (message == null) return;

        var rendered = renderer.renderFinal(screen, finalGuess, questionsAnswered);

        message.editMessageEmbeds(rendered.embed())
                .setComponents(ActionRow.of(
                        Button.secondary(prefixed("done"), "Done").asDisabled()
                ))
                .setFiles(resource(rendered.attachmentName()))
                .queue(null, ignored -> {
                });
    }

    private List<Button> buttonsFor(Query q) {
        // Creates buttons depending on the game state
        if (q instanceof Question) {
            boolean canUndo = questionsAnswered > 0;
            Button undo = Button.secondary(prefixed(UNDO), "Undo");
            if (!canUndo) undo = undo.asDisabled();

            return List.of(
                    Button.primary(prefixed(YES), "Yes"),
                    Button.primary(prefixed(NO), "No"),
                    Button.primary(prefixed(DONT_KNOW), "Don't know"),
                    Button.primary(prefixed(PROBABLY), "Probably"),
                    Button.primary(prefixed(PROBABLY_NOT), "Probably not"),
                    undo,
                    Button.danger(prefixed(STOP), "Stop")
            );
        }
        if (q instanceof Guess) {
            return List.of(
                    Button.primary(prefixed(CONFIRM), "Yes (Correct)"),
                    Button.primary(prefixed(REJECT), "No (Continue)"),
                    Button.danger(prefixed(STOP), "Stop")
            );
        }
        return List.of(Button.danger(prefixed(STOP), "close"));
    }

    private Akiwrapper.Answer mapAnswer(String action) {
        // Maps button action to Akiwrapper answer
        return switch (action) {
            case YES -> Akiwrapper.Answer.YES;
            case NO -> Akiwrapper.Answer.NO;
            case DONT_KNOW -> Akiwrapper.Answer.DONT_KNOW;
            case PROBABLY -> Akiwrapper.Answer.PROBABLY;
            case PROBABLY_NOT -> Akiwrapper.Answer.PROBABLY_NOT;
            default -> null;
        };
    }

    private String prefix() {
        // Unique button id prefix
        return "aki:" + userId + ":" + id + ":";
    }

    private String prefixed(String buttonAction) {
        // Adds prefix to a specific button
        return prefix() + buttonAction;
    }

    private String stripPrefix(String customId) {
        // Removes the prefix from a specific button id so that only the action remains
        String p = prefix();
        if (customId == null || !customId.startsWith(p)) return null;
        return customId.substring(p.length());
    }

    private static FileUpload resource(String name) {
        // Converts a local image to a fileUpload
        InputStream in = AkinatorSession.class.getClassLoader().getResourceAsStream("image/akinator/" + name);
        if (in == null) throw new IllegalStateException("Missing resource: image/akinator/" + name);
        return FileUpload.fromData(in, name);
    }

    private static List<ActionRow> toRows(List<Button> buttons) {
        if (buttons.size() <= 5) return List.of(ActionRow.of(buttons));

        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += 5) {
            rows.add(ActionRow.of(buttons.subList(i, Math.min(i + 5, buttons.size()))));
        }
        return rows;
    }
}