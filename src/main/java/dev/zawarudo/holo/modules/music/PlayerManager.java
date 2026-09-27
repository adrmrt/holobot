package dev.zawarudo.holo.modules.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.Android;
import dev.lavalink.youtube.clients.AndroidVr;
import dev.lavalink.youtube.clients.Ios;
import dev.lavalink.youtube.clients.Web;
import dev.lavalink.youtube.clients.WebEmbedded;
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
        // ANDROID and IOS first: as of 2026-09 the default clients (ANDROID_VR, WEB, WEB_EMBEDDED) fail playback
        audioPlayerManager.registerSourceManager(new YoutubeAudioSourceManager(true,
                new Android(), new Ios(), new AndroidVr(), new Web(), new WebEmbedded()));
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), guildId -> {
            GuildMusicManager guildMusicManager = new GuildMusicManager(audioPlayerManager, guild);
            guild.getAudioManager().setSendingHandler(guildMusicManager.getAudioPlayerHandler());
            return guildMusicManager;
        });
    }

    AudioPlayerManager getAudioPlayerManager() {
        return audioPlayerManager;
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
