SELECT COUNT(*) AS total
FROM PokemonCatches
WHERE user_id = ? AND species_id = ?;
