package dev.zawarudo.holo.modules.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackScheduler.class);

    /**
     * The queue of tracks
     */
    public BlockingQueue<AudioTrack> queue;
    /**
     * The last 10 {@link AudioTrack}s from the {@link AudioPlayer}
     */
    public BlockingQueue<AudioTrack> history;
    public final AudioPlayer audioPlayer;

    // TODO Actually implement looping and paused
    public boolean looping;
    public boolean paused;

    private final Runnable onIdle;
    private final Runnable onActive;

    public TrackScheduler(AudioPlayer player, Runnable onIdle, Runnable onActive) {
        queue = new LinkedBlockingQueue<>();
        history = new LinkedBlockingQueue<>();
        audioPlayer = player;
        looping = false;
        paused = false;
        this.onIdle = onIdle;
        this.onActive = onActive;
    }

    /**
     * Method to add a given {@link AudioTrack} to the queue. If the queue is empty,
     * it will be played by the {@link AudioPlayer}.
     */
    public void enqueue(AudioTrack track) {
        onActive.run();
        if (queue.isEmpty() && audioPlayer.getPlayingTrack() == null) {
            addToHistory(track);
        }
        if (!audioPlayer.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    /**
     * Adds a list of {@link AudioTrack}s to the queue.
     */
    public void enqueue(List<AudioTrack> tracks) {
        for (AudioTrack track : tracks) {
            enqueue(track);
        }
    }

    /**
     * Adds a given {@link AudioTrack} to the start of the queue.
     */
    public void enqueueFirst(AudioTrack track) {
        List<AudioTrack> queueList = new ArrayList<>(queue);
        queue.clear();
        queue.add(track);
        queue.addAll(queueList);
    }

    /**
     * Method to shuffle the queue, i.e. the order of {@link AudioTrack}s will be
     * randomized.
     */
    public void shuffle() {
        List<AudioTrack> queueList = new ArrayList<>(queue);
        queue.clear();
        Collections.shuffle(queueList);
        queue.addAll(queueList);
    }

    /**
     * Method to add a given {@link AudioTrack} to the history. Note that the history can
     * only contain 10 items, meaning the oldest ones will be removed.
     */
    public void addToHistory(AudioTrack track) {
        List<AudioTrack> historyList = new ArrayList<>(history);
        history.clear();
        historyList.addFirst(track);
        while (historyList.size() > 10) {
            historyList.remove(10);
        }
        for (AudioTrack t : historyList) {
            history.offer(t);
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        LOGGER.error("Track '{}' threw an exception (severity={})", track.getInfo().title, exception.severity, exception);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        LOGGER.warn("Track '{}' is stuck (threshold={}ms)", track.getInfo().title, thresholdMs);
    }

    /**
     * Method which decides what happens after the current {@link AudioTrack}
     * finishes.
     */
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            if (looping) {
                audioPlayer.startTrack(track.makeClone(), false);
            } else {
                playNext();
            }
        }
    }

    /**
     * Plays the next {@link AudioTrack} in the queue. If the queue is empty, the
     * {@link AudioPlayer} will simply stop.
     */
    public void playNext() {
        AudioTrack track = queue.poll();
        audioPlayer.startTrack(track, false);
        if (track != null) {
            addToHistory(track);
        } else {
            onIdle.run();
        }
    }
}
