package dev.zawarudo.holo.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlywayMigrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayMigrator.class);

    private FlywayMigrator() {
        throw new UnsupportedOperationException();
    }

    public static void migrate(@NotNull String dbPath) throws FlywayException {
        if (dbPath.isBlank()) {
            throw new IllegalArgumentException("dbPath must be non-blank");
        }

        String url = "jdbc:sqlite:" + dbPath.trim();

        Flyway flyway = Flyway.configure()
            .dataSource(url, null, null)
            .locations("classpath:db/migration")
            .cleanDisabled(true)
            .baselineOnMigrate(true)
            .baselineVersion("1")
            .validateMigrationNaming(true)
            .load();

        LOGGER.info("Running DB migrations...");
        var result = flyway.migrate();
        LOGGER.info("Migrations complete. Schema now at version {} ({} migrations applied).",
            result.targetSchemaVersion, result.migrationsExecuted);
    }
}
