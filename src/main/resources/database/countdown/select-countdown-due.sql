SELECT * FROM Countdowns WHERE date_time <= ? AND notified = 0 AND channel_id IS NOT NULL;
