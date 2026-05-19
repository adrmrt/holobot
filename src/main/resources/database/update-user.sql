UPDATE DiscordUsers
SET username      = ?,
    is_bot        = ?
WHERE user_id = ?;