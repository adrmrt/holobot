package dev.zawarudo.holo.modules.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that manages the music players across all guilds.
 */
public class PlayerManager {

	private static PlayerManager instance;
	private final Map<Long, GuildMusicManager> musicManagers;
	private final AudioPlayerManager audioPlayerManager;

	public PlayerManager() {
		musicManagers = new HashMap<>();
		audioPlayerManager = new DefaultAudioPlayerManager();
		audioPlayerManager.registerSourceManager(new YoutubeAudioSourceManager());
		AudioSourceManagers.registerRemoteSources(audioPlayerManager);
		AudioSourceManagers.registerLocalSource(audioPlayerManager);
	}

	public GuildMusicManager getMusicManager(Guild guild) {
		return this.musicManagers.computeIfAbsent(guild.getIdLong(), guildId -> {
			GuildMusicManager guildMusicManager = new GuildMusicManager(audioPlayerManager);
			guild.getAudioManager().setSendingHandler(guildMusicManager.getAudioPlayerHandler());
			return guildMusicManager;
		});
	}

	public void loadAndPlay(Guild guild, String trackUrl, AudioLoadResultHandler audioLoadResultHandler) {
		GuildMusicManager musicManager = getMusicManager(guild);
		audioPlayerManager.loadItemOrdered(musicManager, trackUrl, audioLoadResultHandler);
	}

	/**
	 * Returns the instance of this class.
	 */
	public static PlayerManager getInstance() {
		if (PlayerManager.instance == null) {
			PlayerManager.instance = new PlayerManager();
		}
		return PlayerManager.instance;
	}
}