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

    /**
     * Whether {@code viewerId} (viewing from {@code viewerGuildId}, or {@code 0} outside a guild)
     * is allowed to see this countdown: the owner always can, and anyone in the same guild can see
     * {@code PUBLIC} or {@code GLOBAL} countdowns created there.
     */
    public boolean isVisibleTo(long viewerId, long viewerGuildId) {
        if (userId == viewerId) {
            return true;
        }
        boolean sharedInGuild = visibility == Visibility.GLOBAL || visibility == Visibility.PUBLIC;
        return sharedInGuild && viewerGuildId != 0 && viewerGuildId == guildId;
    }
}
