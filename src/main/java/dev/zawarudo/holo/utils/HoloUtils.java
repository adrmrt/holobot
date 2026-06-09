package dev.zawarudo.holo.utils;

import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Class containing miscellaneous utility methods.
 */
public final class HoloUtils {

    private HoloUtils() {
    }

    /**
     * Returns a list of {@link Emote}s representing the numbers one to ten.
     */
    public static @NotNull List<Emote> getNumbers() {
        return new ArrayList<>(List.of(
            Emote.ONE, Emote.TWO, Emote.THREE, Emote.FOUR, Emote.FIVE,
            Emote.SIX, Emote.SEVEN, Emote.EIGHT, Emote.NINE, Emote.TEN
        ));
    }

    /**
     * Returns a list of {@link Emote}s representing the numbers one to ten.
     *
     * @param count The number of elements to return.
     */
    public static List<Emote> getNumbers(int count) {
        return getNumbers().subList(0, count);
    }

    public static void addReactions(Message msg, int count) {
        for (Emote emote : HoloUtils.getNumbers(count)) {
            msg.addReaction(emote.getAsEmoji()).queue(s -> {
            }, e -> {
            });
        }
    }
}
