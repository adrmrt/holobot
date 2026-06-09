package dev.zawarudo.holo.core;

/**
 * Immutable configuration for the Holo bot.
 * <p>
 * Prefer building this from environment variables (e.g., Docker Compose)
 * or a secure secret source. This class is deliberately immutable to
 * prevent accidental runtime mutation of sensitive settings.
 */
public final class BotConfig {

    private final String botToken;
    private final long ownerId;

    private final String deepAIKey;
    private final String aocToken;
    private final String saucenaoToken;
    private final String githubToken;
    private final String dictionaryKey;
    private final String thesaurusKey;

    private final String defaultPrefix;
    private final String dbPath;

    private BotConfig(
        String botToken,
        long ownerId,
        String deepAIKey,
        String aocToken,
        String saucenaoToken,
        String githubToken,
        String dictionaryKey,
        String thesaurusKey,
        String defaultPrefix,
        String dbPath
    ) {
        this.botToken = requireNonBlank(botToken, "botToken");
        this.ownerId = ownerId;
        this.deepAIKey = nullToEmpty(deepAIKey);
        this.aocToken = nullToEmpty(aocToken);
        this.saucenaoToken = nullToEmpty(saucenaoToken);
        this.githubToken = nullToEmpty(githubToken);
        this.dictionaryKey = dictionaryKey;
        this.thesaurusKey = thesaurusKey;
        this.defaultPrefix = defaultPrefix == null || defaultPrefix.isBlank() ? "<" : defaultPrefix;
        this.dbPath = dbPath;
    }

    /**
     * Discord bot token.
     */
    public String getBotToken() {
        return botToken;
    }

    /**
     * Discord user ID of the bot owner.
     */
    public long getOwnerId() {
        return ownerId;
    }

    /**
     * DeepAI API key.
     */
    public String getDeepAIKey() {
        return deepAIKey;
    }

    /**
     * Advent of Code session token.
     */
    public String getAocToken() {
        return aocToken;
    }

    /**
     * SauceNAO token.
     */
    public String getSaucenaoToken() {
        return saucenaoToken;
    }

    /**
     * GitHub token.
     */
    public String getGitHubToken() {
        return githubToken;
    }

    /**
     * Dictionary key.
     */
    public String getDictionaryKey() {
        return dictionaryKey;
    }

    /**
     * Thesaurus key.
     */
    public String getThesaurusKey() {
        return thesaurusKey;
    }

    /**
     * Default command prefix (defaults to {@code "<"}).
     */
    public String getDefaultPrefix() {
        return defaultPrefix;
    }

    /**
     * Path to the database.
     */
    public String getDbPath() {
        return dbPath;
    }

    /**
     * Convenience builder for {@link BotConfig}.
     */
    public static final class Builder {
        private String botToken;
        private Long ownerId;
        private String deepAIKey;
        private String aocToken;
        private String saucenaoToken;
        private String githubToken;
        private String dictionaryKey;
        private String thesaurusKey;
        private String defaultPrefix = "<";
        private String dbPath;

        public Builder botToken(String botToken) {
            this.botToken = botToken;
            return this;
        }

        public Builder ownerId(long ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder deepAIKey(String deepAIKey) {
            this.deepAIKey = deepAIKey;
            return this;
        }

        public Builder aocToken(String aocToken) {
            this.aocToken = aocToken;
            return this;
        }

        public Builder saucenaoToken(String saucenaoToken) {
            this.saucenaoToken = saucenaoToken;
            return this;
        }

        public Builder githubToken(String githubToken) {
            this.githubToken = githubToken;
            return this;
        }

        public Builder dictionaryKey(String dictionaryKey) {
            this.dictionaryKey = dictionaryKey;
            return this;
        }

        public Builder thesaurusKey(String thesaurusKey) {
            this.thesaurusKey = thesaurusKey;
            return this;
        }

        public Builder defaultPrefix(String defaultPrefix) {
            this.defaultPrefix = defaultPrefix;
            return this;
        }

        public Builder dbPath(String dbPath) {
            this.dbPath = dbPath;
            return this;
        }

        public BotConfig build() {
            if (ownerId == null) {
                throw new IllegalStateException("ownerId is required");
            }
            return new BotConfig(
                botToken,
                ownerId,
                deepAIKey,
                aocToken,
                saucenaoToken,
                githubToken,
                dictionaryKey,
                thesaurusKey,
                defaultPrefix,
                dbPath
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        // Mask secrets to avoid leaking them in logs
        return "BotConfig{" +
            "botToken=" + mask(botToken) +
            ", ownerId=" + ownerId +
            ", deepAIKey=" + mask(deepAIKey) +
            ", aocToken=" + mask(aocToken) +
            ", saucenaoToken=" + mask(saucenaoToken) +
            ", githubToken=" + mask(githubToken) +
            ", dictionaryKey=" + mask(dictionaryKey) +
            ", thesaurusKey=" + mask(thesaurusKey) +
            ", defaultPrefix='" + defaultPrefix + '\'' +
            ", dbPath='" + dbPath + '\'' +
            '}';
    }

    private static String requireNonBlank(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(name + " must be provided and non-blank");
        }
        return v;
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    static String mask(String v) {
        if (v == null || v.isBlank()) return "";
        int keep = Math.min(4, v.length());
        return "*".repeat(Math.max(0, v.length() - keep)) + v.substring(v.length() - keep);
    }
}
