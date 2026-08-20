SELECT c.*
FROM XkcdComics_fts f
         JOIN XkcdComics c ON c.id = f.rowid
         LEFT JOIN (SELECT rowid
                    FROM XkcdComics_fts
                    WHERE XkcdComics_fts MATCH ?) p ON p.rowid = f.rowid
WHERE XkcdComics_fts MATCH ?
ORDER BY (p.rowid IS NULL) ASC,
         bm25(XkcdComics_fts)
LIMIT ? OFFSET ?;
