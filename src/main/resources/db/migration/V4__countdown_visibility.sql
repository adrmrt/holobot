--- Create the Countdowns table if it doesn't exist
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
