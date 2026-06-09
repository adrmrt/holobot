package dev.zawarudo.holo.modules.akinator;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.eu.zajc.akiwrapper.core.entities.Guess;
import org.eu.zajc.akiwrapper.core.entities.Query;
import org.eu.zajc.akiwrapper.core.entities.Question;

import java.time.Instant;

public final class AkinatorEmbedRenderer {

    public static final String IMG_START = "akinator_start.png";
    public static final String IMG_DEFAULT = "akinator_icon.png";
    public static final String IMG_GUESSING = "akinator_guessing.png";
    public static final String IMG_VICTORY = "akinator_victory.png";
    public static final String IMG_DEFEAT = "akinator_defeat.png";
    public static final String IMG_CANCEL = "akinator_cancel.png";

    public record Rendered(MessageEmbed embed, String attachmentName) {
    }

    public Rendered renderStart() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Akinator");
        eb.setTimestamp(Instant.now());

        eb.setDescription("""
            To start the game, think of a real or fictional character.
            I will ask questions and try to guess who it is.

            Press **Start** when you're ready, or **Cancel** to stop.
            """);

        String attach = IMG_DEFAULT;
        eb.setThumbnail("attachment://" + attach);
        return new Rendered(eb.build(), attach);
    }

    public Rendered render(Query currentQuery, long questionsAnswered) {
        EmbedBuilder eb = base();

        String attach;

        if (currentQuery instanceof Question question) {
            long n = questionsAnswered + 1;

            eb.setDescription("**Question " + n + ":** " + question.getText());

            attach = IMG_START;
            eb.setThumbnail("attachment://" + attach);

        } else if (currentQuery instanceof Guess guess) {
            eb.setDescription("**I think of ** " + guess.getName() + "?\n"
                + (!guess.getDescription().isBlank() ? "\n" + guess.getDescription() : ""));

            var img = guess.getImage();
            if (img != null) {
                String url = img.toString();
                if (!url.isBlank()) eb.setImage(url);
            }

            attach = IMG_GUESSING;
            eb.setThumbnail("attachment://" + attach);

        } else {
            eb.setDescription("I ran out of questions.");
            attach = IMG_DEFEAT;
            eb.setThumbnail("attachment://" + attach);
        }

        return new Rendered(eb.build(), attach);
    }

    public Rendered renderFinal(EndScreen endScreen, Guess finalGuess, long questionsAnswered) {
        EmbedBuilder eb = base();

        String description = switch (endScreen) {
            case VICTORY -> "Great, guessed right one more time!";
            case DEFEAT -> "I ran out of questions — you win!";
            case CANCEL -> "You cancelled the game.";
            case TIMEOUT -> "Game timed out.";
        };

        if (endScreen == EndScreen.VICTORY && finalGuess != null) {
            if (questionsAnswered >= 0) {
                description += "\n\nGuessed in **" + questionsAnswered + "** questions.";
            }

            description += "\n\n" + finalGuess.getName();

            if (!finalGuess.getDescription().isBlank()) {
                description += "\n" + finalGuess.getDescription();
            }

            var img = finalGuess.getImage();
            if (img != null && !img.toString().isBlank()) {
                eb.setImage(img.toString()); // embed image (NOT thumbnail)
            }
        }

        eb.setDescription(description);

        String attach = switch (endScreen) {
            case VICTORY -> IMG_VICTORY;
            case DEFEAT -> IMG_DEFEAT;
            case CANCEL, TIMEOUT -> IMG_CANCEL;
        };

        eb.setThumbnail("attachment://" + attach);
        return new Rendered(eb.build(), attach);
    }

    public enum EndScreen {VICTORY, DEFEAT, CANCEL, TIMEOUT}

    private EmbedBuilder base() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Akinator");
        eb.setTimestamp(Instant.now());
        return eb;
    }
}
