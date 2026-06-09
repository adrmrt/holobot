WITH FilteredEmotes AS (SELECT emote_id,
                               emote_name,
                               image_url,
                               created_at,
                               LOWER(emote_name) AS normalized_name
                        FROM Emotes
                        WHERE emote_name LIKE '%lick%' COLLATE NOCASE -- Case-insensitive search
),
     RankedEmotes AS (SELECT emote_id,
                             emote_name,
                             image_url,
                             created_at,
                             normalized_name,
                             ROW_NUMBER() OVER (
                                 PARTITION BY normalized_name
                                 ORDER BY created_at, emote_id
                                 )                                        AS row_number,
                             COUNT(*) OVER (PARTITION BY normalized_name) AS name_count
                      FROM FilteredEmotes)
SELECT emote_id,
       emote_name,
       image_url,
       CASE
           WHEN name_count = 1 THEN emote_name
           WHEN row_number = 1 THEN emote_name
           ELSE emote_name || '-' || (row_number - 1)
           END AS display_name
FROM RankedEmotes
ORDER BY normalized_name, row_number;
