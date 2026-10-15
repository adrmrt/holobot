SELECT COALESCE(SUM(points), 0) AS points
FROM PokemonCatches
WHERE guild_id = ? AND user_id = ?;
