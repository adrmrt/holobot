package dev.zawarudo.holo.modules.countdown;

public record Countdown(long id, String name, long timeCreated, long dateTime, long userId, long guildId,
                         Visibility visibility, long channelId, boolean notified) {

    /**
     * Who can see a countdown.
     */
    public enum Visibility {
        /** Only visible to the creator, e.g. in DMs. */
        PRIVATE,
        /** Visible to anyone in the creator's guild via {@code countdown <id>}. */
        PUBLIC,
        /** Visible to everyone in the guild via {@code countdown all} or {@code countdown <id>}. */
        GLOBAL
    }
}
