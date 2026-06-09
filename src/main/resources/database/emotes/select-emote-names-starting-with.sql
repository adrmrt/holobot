SELECT emote_name
FROM Emotes
WHERE LOWER(emote_name) LIKE ?;
