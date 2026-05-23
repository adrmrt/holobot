package dev.zawarudo.holo.modules.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GuildMusicManager {

	/** Minutes of silence before the bot disconnects automatically. */
	private static final long IDLE_TIMEOUT_MINUTES = 5;

	public final AudioPlayer audioPlayer;
	public final TrackScheduler scheduler;
	private final AudioPlayerSendHandler audioPlayerHandler;
	private final Guild guild;
	private boolean voting;
	private final AtomicInteger counter;
	private CompletableFuture<Void> autoLeaveTask;

	public GuildMusicManager(AudioPlayerManager manager, Guild guild) {
		this.guild = guild;
		audioPlayer = manager.createPlayer();
		scheduler = new TrackScheduler(audioPlayer, this::scheduleAutoLeave, this::cancelAutoLeave);
		audioPlayer.addListener(scheduler);
		audioPlayerHandler = new AudioPlayerSendHandler(audioPlayer);
		voting = false;
		counter = new AtomicInteger(0);
	}

	private void scheduleAutoLeave() {
		cancelAutoLeave();
		autoLeaveTask = CompletableFuture.runAsync(
				() -> {
					if (!guild.getAudioManager().isConnected()) return;
					// Guard against cancellation losing the race: abort if playback resumed
					if (audioPlayer.getPlayingTrack() != null || !scheduler.queue.isEmpty()) return;
					// Bot is confirmed idle - no need to call clear(), just disconnect
					CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
							.execute(() -> guild.getAudioManager().closeAudioConnection());
				},
				CompletableFuture.delayedExecutor(IDLE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
		);
	}

	private void cancelAutoLeave() {
		if (autoLeaveTask != null) {
			autoLeaveTask.cancel(false);
		}
	}

	/**
	 * Returns the instance of {@link AudioPlayerSendHandler}.
	 */
	public AudioPlayerSendHandler getAudioPlayerHandler() {
		return audioPlayerHandler;
	}

	/**
	 * Resets the voting.
	 */
	public void resetVoting() {
		voting = false;
		counter.set(0);
	}

	/**
	 * Sets the voting variable that keeps track of whether there is a voting ongoing.
	 *
	 * @param isVoting Whether the guild is voting or not.
	 */
	public void setVoting(boolean isVoting) {
		voting = isVoting;
	}

	/**
	 * Checks if there is a voting session going on.
	 */
	public boolean isVoting() {
		return voting;
	}

	/**
	 * Returns a counter that is used to determine if a vote has passed.
	 */
	public AtomicInteger getVoteCounter() {
		return counter;
	}
	
	/**
	 * Stops playback and clears queue state without arming the idle-disconnect timer.
	 * Use this when disconnecting immediately (e.g. empty VC, explicit leave).
	 */
	public void stop() {
		cancelAutoLeave();
		scheduler.looping = false;
		scheduler.queue.clear();
		audioPlayer.stopTrack();
	}

	/**
	 * Stops playback, clears queue state, and arms the idle-disconnect timer.
	 * Use this when the bot stays in the VC but should leave if it remains idle.
	 */
	public void clear() {
		stop();
		scheduleAutoLeave();
	}
}