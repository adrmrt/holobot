package dev.zawarudo.holo.core;

import dev.zawarudo.holo.database.Database;
import dev.zawarudo.holo.database.FlywayMigrator;
import dev.zawarudo.holo.utils.VersionInfo;
import io.github.cdimascio.dotenv.Dotenv;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static dev.zawarudo.holo.core.BotConfig.mask;

/**
 * Main class of the application.
 */
public final class Bootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);
    private static Dotenv dotenv;

    /**
     * The exact time at which the bot started up in milliseconds.
     */
    private static long startupTime;

    /**
     * The running bot instance (initialized in {@link #init()}).
     */
    public static Holo holo;

    private Bootstrap() {
    }

    public static void main(String[] args) {
        init();
    }

    private static void init() {
        startupTime = System.currentTimeMillis();

        dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .ignoreIfMalformed()
            .load();

        LOGGER.info(".env loaded ({} entries)", dotenv.entries().size());

        LOGGER.info("BOT_TOKEN: {}", mask(dotenv.get("BOT_TOKEN")));
        LOGGER.info("OWNER_ID: {}", dotenv.get("OWNER_ID"));

        String version = VersionInfo.getBotVersion();
        LOGGER.info("Version: {}", version);

        BotConfig config = buildConfig(dotenv);

        checkDatabase();

        // Create bot instance
        holo = new Holo(config);
        holo.registerEarlyManagers();

        if (LOGGER.isInfoEnabled()) {
            long totalTime = System.currentTimeMillis() - startupTime;
            LOGGER.info("It took {} {} ms to load!", holo.getJDA().getSelfUser().getName(), totalTime);
        }
    }

    /**
     * Deserializes the config file to a {@link BotConfig} object.
     *
     * @return A {@link BotConfig} object.
     */
    private static BotConfig buildConfig(Dotenv env) {
        return BotConfig.builder()
            .botToken(require(env, "BOT_TOKEN"))
            .ownerId(requireLong(env, "OWNER_ID"))
            .defaultPrefix(env.get("DEFAULT_PREFIX", "<"))
            .deepAIKey(env.get("DEEP_AI_TOKEN", ""))
            .aocToken(env.get("AOC_TOKEN", ""))
            .saucenaoToken(env.get("SAUCE_NAO_TOKEN", ""))
            .githubToken(env.get("GITHUB_TOKEN", ""))
            .dictionaryKey(env.get("KEY_DICTIONARY", ""))
            .thesaurusKey(env.get("KEY_THESAURUS", ""))
            .build();
    }

    private static void checkDatabase() {
        String envPath = dotenv.get("DB_PATH");
        if (envPath != null && !envPath.isBlank()) {
            Database.setDbPath(envPath);
        }

        String dbPath = Database.getDbPath();

        File dbFile = new File(dbPath);
        File parent = dbFile.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create DB directory: " + parent.getAbsolutePath());
        }

        // Flyway creates the DB file if needed and applies migrations.
        try {
            FlywayMigrator.migrate(dbPath);
        } catch (FlywayException ex) {
            throw new IllegalStateException("Failed to run DB migrations for " + dbFile.getAbsolutePath(), ex);
        }

        LOGGER.info("Database ready at {}", dbFile.getAbsolutePath());
    }

    /**
     * Restarts the bot.
     */
    public static void restart() {
        LOGGER.info("Restarting...");
        shutdown();
        init();
    }

    /**
     * Shuts the bot down.
     */
    public static void shutdown() {
        LOGGER.info("Shutting down...");
        if (holo != null) {
            holo.close();
        }
    }

    public static long getStartupTime() {
        return startupTime;
    }

    private static String require(Dotenv env, String name) {
        String val = env.get(name);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return val;
    }

    private static long requireLong(Dotenv env, String name) {
        String val = require(env, name);
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Environment variable '" + name + "' must be a valid long, but was: '" + val + "'", e);
        }
    }
}
