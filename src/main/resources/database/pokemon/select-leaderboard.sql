SELECT user_id, SUM(points) AS points
FROM PokemonCatches
WHERE guild_id = ?
GROUP BY user_id
ORDER BY points DESC, user_id;
