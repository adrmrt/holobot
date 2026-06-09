SELECT emote_id, emote_name, LOWER(emote_name) AS base_name
FROM Emotes
WHERE LOWER(emote_name) IN (SELECT LOWER(emote_name)
                            FROM Emotes
                            GROUP BY LOWER(emote_name)
                            HAVING COUNT(*) > 1)
ORDER BY base_name, emote_id;
