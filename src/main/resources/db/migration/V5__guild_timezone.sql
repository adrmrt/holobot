--- Add the guild-configured reference timezone used for date/time command input
ALTER TABLE DiscordGuildConfigs ADD COLUMN timezone TEXT NOT NULL DEFAULT 'UTC';
