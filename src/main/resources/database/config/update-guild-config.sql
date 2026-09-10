UPDATE DiscordGuildConfigs
SET prefix           = ?,
    nsfw             = ?,
    disabled_modules = ?,
    timezone         = ?
WHERE guild_id = ?;
