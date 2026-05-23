package dev.zawarudo.holo.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.modules.music.GuildMusicManager;
import dev.zawarudo.holo.modules.music.PlayerManager;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@CommandInfo(name = "clone",
		description = "Duplicates the currently playing track and adds it on top of the queue.",
		category = CommandCategory.MUSIC)
public class CloneCmd extends AbstractMusicCommand {

	@Override
	public void onCommand(@NotNull MessageReceivedEvent event) {
		deleteInvoke(event);

		GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());

		AudioTrack current = musicManager.audioPlayer.getPlayingTrack();

		// Check if there are any tracks playing
		if (current == null) {
			sendErrorEmbed(event, "I'm not playing any tracks at the moment!");
			return;
		}

		// Check vc conditions (user and bot in same vc, etc.)
		if (!isUserInSameAudioChannel(event)) {
			sendErrorEmbed(event, "You need to be in the same voice channel as me to use this command!");
			return;
		}

		musicManager.scheduler.enqueueFirst(current.makeClone());

		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("Cloned track");
		if (current.getInfo().artworkUrl != null) builder.setThumbnail(current.getInfo().artworkUrl);
		builder.addField("Title", current.getInfo().title, false);
		builder.addField("Uploader", current.getInfo().author, false);
		builder.addField("Link", "[Open](" + current.getInfo().uri + ")", false);

		sendEmbed(event, builder, true, 1, TimeUnit.MINUTES);
	}
}