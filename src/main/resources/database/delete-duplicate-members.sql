DELETE
FROM DiscordGuildUsers
WHERE ROWID NOT IN (SELECT min(ROWID) FROM DiscordGuildUsers GROUP BY guild_id, user_id);