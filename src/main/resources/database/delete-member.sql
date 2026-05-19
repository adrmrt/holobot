DELETE
FROM DiscordGuildUsers
WHERE guild_id = ?
  AND user_id = ?;