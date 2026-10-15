SELECT species_id, MAX(name) AS name, COUNT(*) AS total, SUM(shiny) AS shinies
FROM PokemonCatches
WHERE user_id = ?
GROUP BY species_id
ORDER BY species_id;
