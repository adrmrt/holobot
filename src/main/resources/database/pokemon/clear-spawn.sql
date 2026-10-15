UPDATE PokemonSpawns
SET pokemon_id = NULL, shiny = 0, message_id = NULL, spawned_at = NULL
WHERE channel_id = ?;
