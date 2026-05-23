package dev.zawarudo.holo.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.utils.Emote;
import dev.zawarudo.holo.modules.music.GuildMusicManager;
import dev.zawarudo.holo.modules.music.PlayerManager;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@CommandInfo(name = "now",
		description = "Shows information about the current track.",
		alias = {"np", "nowplaying"},
		category = CommandCategory.MUSIC)
public class NowPlayingCmd extends AbstractMusicCommand {

	@Override
	public void onCommand(@NotNull MessageReceivedEvent e) {
		deleteInvoke(e);
		
		GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(e.getGuild());
		AudioPlayer audioPlayer = musicManager.audioPlayer;
		
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle(Emote.SPEAKER_LOUD.getAsText() + " Track Information");
		
		if (audioPlayer.getPlayingTrack() == null) {
			builder.setDescription("I'm not playing any tracks right now");
			sendEmbed(e, builder, true, 15, TimeUnit.SECONDS);
			return;
		}
		
		AudioTrackInfo info = audioPlayer.getPlayingTrack().getInfo();
		
		String timestamp = "[`" + Formatter.formatTrackTime(audioPlayer.getPlayingTrack().getPosition()) + "`|`"
				+ Formatter.formatTrackTime(audioPlayer.getPlayingTrack().getDuration()) + "`]";
		
		if (info.artworkUrl != null) builder.setThumbnail(info.artworkUrl);
		builder.addField("Title", info.title, false);
		builder.addField("Current Timestamp", timestamp, true);
		builder.addField("Link", "[Open](" + info.uri + ")", false);
		
		sendEmbed(e, builder, true, 1, TimeUnit.MINUTES);
	}
}