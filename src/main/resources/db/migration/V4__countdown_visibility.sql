-- Installs baselined at version 1 (baselineOnMigrate) never actually ran V1's SQL, so
-- Countdowns may not exist yet even though "current" V1 creates it. Recreate it here with
-- its original V1 shape before altering, so both real-V1 and baselined installs converge.
CREATE TABLE IF NOT EXISTS Countdowns
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT,
    time_created INTEGER,
    date_time    INTEGER,
    user_id      INTEGER,
    guild_id     INTEGER,
    FOREIGN KEY (user_id) REFERENCES DiscordUsers,
    FOREIGN KEY (guild_id) REFERENCES DiscordGuilds
);

ALTER TABLE Countdowns ADD COLUMN visibility TEXT NOT NULL DEFAULT 'PRIVATE';
ALTER TABLE Countdowns ADD COLUMN channel_id INTEGER;
ALTER TABLE Countdowns ADD COLUMN notified INTEGER NOT NULL DEFAULT 0;

ALTER TABLE DiscordGuildConfigs ADD COLUMN timezone TEXT NOT NULL DEFAULT 'UTC';
