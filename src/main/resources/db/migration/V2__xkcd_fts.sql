-- Full-text search index for XKCD comics
CREATE VIRTUAL TABLE IF NOT EXISTS XkcdComics_fts
    USING fts5
(
    title,
    alt,
    content='XkcdComics',
    content_rowid='id',
    tokenize='unicode61'
);

-- Insert trigger
CREATE TRIGGER IF NOT EXISTS XkcdComics_ai
    AFTER INSERT
    ON XkcdComics
BEGIN
    INSERT INTO XkcdComics_fts(rowid, title, alt)
    VALUES (new.id, new.title, new.alt);
END;

-- Delete trigger
CREATE TRIGGER IF NOT EXISTS XkcdComics_ad
    AFTER DELETE
    ON XkcdComics
BEGIN
    INSERT INTO XkcdComics_fts(XkcdComics_fts, rowid, title, alt)
    VALUES ('delete', old.id, old.title, old.alt);
END;

-- Update trigger
CREATE TRIGGER IF NOT EXISTS XkcdComics_au
    AFTER UPDATE
    ON XkcdComics
BEGIN
    INSERT INTO XkcdComics_fts(XkcdComics_fts, rowid, title, alt)
    VALUES ('delete', old.id, old.title, old.alt);

    INSERT INTO XkcdComics_fts(rowid, title, alt)
    VALUES (new.id, new.title, new.alt);
END;

-- Build index for existing rows
INSERT INTO XkcdComics_fts(XkcdComics_fts)
VALUES ('rebuild');
