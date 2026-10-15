--- Pokemon spawn channels with the wild Pokemon currently waiting in each, and every successful catch
CREATE TABLE IF NOT EXISTS PokemonSpawns
(
    channel_id INTEGER PRIMARY KEY,
    guild_id   INTEGER NOT NULL,
    pokemon_id INTEGER,
    shiny      INTEGER NOT NULL DEFAULT 0,
    message_id INTEGER,
    spawned_at INTEGER
);

CREATE TABLE IF NOT EXISTS PokemonCatches
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL,
    guild_id   INTEGER NOT NULL,
    species_id INTEGER NOT NULL,
    name       TEXT    NOT NULL,
    shiny      INTEGER NOT NULL DEFAULT 0,
    points     INTEGER NOT NULL,
    caught_at  INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pokemon_catches_user ON PokemonCatches (user_id, species_id);
CREATE INDEX IF NOT EXISTS idx_pokemon_catches_guild ON PokemonCatches (guild_id, user_id);
