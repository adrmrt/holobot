package dev.zawarudo.holo.modules.music;

import dev.lavalink.youtube.AllClientsFailedException;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Checks that YouTube tracks load and stream with the source configuration in {@link PlayerManager}.
 */
class PlayerManagerIT {

    // "Me at the zoo": first YouTube video, 19 seconds long
    private static final String VIDEO_URL = "https://www.youtube.com/watch?v=jNQXAC9IVRw";
    // Random music playlist
    private static final String PLAYLIST_URL = "https://www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI";

    // 250 frames of 20 ms = 5 seconds of audio
    private static final int MIN_FRAMES = 250;

    private final AudioPlayerManager manager = new PlayerManager().getAudioPlayerManager();

    // Loading can succeed while playback fails, so this pulls actual audio frames
    @Test
    @Timeout(60)
    void video_streamsAudio() throws Exception {
        AudioItem item;
        try {
            item = manager.loadItemSync(VIDEO_URL);
        } catch (AllClientsFailedException e) {
            // YouTube puts a login wall in front of datacenter IPs such as GitHub runners, which says nothing about our code
            assumeFalse(System.getenv("CI") != null && e.getMessage().contains("requires login"),
                    "YouTube requires login from this CI runner's IP");
            throw e;
        }
        AudioTrack track = assertInstanceOf(AudioTrack.class, item);

        AudioPlayer player = manager.createPlayer();
        AtomicReference<FriendlyException> error = new AtomicReference<>();
        player.addListener(new AudioEventAdapter() {
            @Override
            public void onTrackException(AudioPlayer p, AudioTrack t, FriendlyException e) {
                error.set(e);
            }
        });
        player.playTrack(track);

        int frames = 0;
        while (frames < MIN_FRAMES && error.get() == null && player.getPlayingTrack() != null) {
            if (player.provide(1, TimeUnit.SECONDS) != null) {
                frames++;
            }
        }
        player.destroy();

        if (error.get() != null) {
            fail("Playback failed after " + frames + " frames", error.get());
        }
        assertEquals(MIN_FRAMES, frames, "Track ended before enough audio was streamed");
    }

    @Test
    @Timeout(30)
    void playlist_loadsTracks() {
        AudioItem item = manager.loadItemSync(PLAYLIST_URL);
        AudioPlaylist playlist = assertInstanceOf(AudioPlaylist.class, item);

        assertFalse(playlist.getTracks().isEmpty());
    }
}
