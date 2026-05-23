package dev.zawarudo.holo.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.modules.music.GuildMusicManager;
import dev.zawarudo.holo.modules.music.PlayerManager;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "play",
        description = "Play a track or playlist from YouTube, SoundCloud, Bandcamp, Vimeo, Twitch, or a direct HTTP URL. Adds to the queue if something is already playing.",
        usage = "<url>",
        alias = {"p"},
        category = CommandCategory.MUSIC)
public class PlayCmd extends AbstractMusicCommand {

    @Override
    public void onCommand(@NotNull MessageReceivedEvent e) {
        deleteInvoke(e);

        EmbedBuilder builder = new EmbedBuilder();
        String footerText = String.format("Invoked by %s", e.getMember() != null ? e.getMember().getEffectiveName() : e.getAuthor().getName());
        builder.setFooter(footerText, e.getAuthor().getEffectiveAvatarUrl());

        if (!isUserInSameAudioChannel(e)) {
            builder.setTitle("Not in same voice channel!");
            builder.setDescription("You need to be in the same voice channel as me!");
            sendEmbed(e, builder, false, 15, TimeUnit.SECONDS);
            return;
        }

        if (args.length == 0) {
            builder.setTitle("Incorrect Usage");
            builder.setDescription("Please provide a link!");
            sendEmbed(e, builder, false, 15, TimeUnit.SECONDS);
            return;
        }

        String link = args[0].replace("<", "").replace(">", "");

        if (!isValidUrl(link)) {
            builder.setTitle("Invalid Link");
            builder.setDescription("Please provide a valid link!");
            sendEmbed(e, builder, false, 15, TimeUnit.SECONDS);
            return;
        }

        AudioLoadResultHandler audioLoadResultHandler = getAudioLoadResultHandler(e, builder, link);
        PlayerManager.getInstance().loadAndPlay(e.getGuild(), link, audioLoadResultHandler);
    }

    /**
     * Creates an {@link AudioLoadResultHandler} object that handles the result of the audio loading and returns it.
     */
    private AudioLoadResultHandler getAudioLoadResultHandler(MessageReceivedEvent e, EmbedBuilder builder, String link) {
        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(e.getGuild());

        return new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.scheduler.enqueue(track);

                builder.setTitle("Added to the queue");
                String artworkUrl = track.getInfo().artworkUrl;
                if (artworkUrl != null) builder.setThumbnail(artworkUrl);
                builder.addField("Title", track.getInfo().title, false);
                builder.addField("Uploader", track.getInfo().author, false);
                builder.addField("Link", "[Open](" + link + ")", false);
                sendEmbed(e, builder, true, 1, TimeUnit.MINUTES);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                musicManager.scheduler.enqueue(tracks);

                builder.setTitle("Added to the queue");
                builder.setDescription("`" + tracks.size() + "` tracks from playlist `" + playlist.getName() + "`");
                builder.addField("Link", "[Open](" + link + ")", false);
                sendEmbed(e, builder, true, 1, TimeUnit.MINUTES);
            }

            @Override
            public void noMatches() {
                builder.setTitle("No matches!");
                builder.setDescription("I couldn't find any matches for the given link! Please make sure it's a valid link and try again.");
                sendEmbed(e, builder, true, 1, TimeUnit.MINUTES);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                builder.setTitle("Load failed!");
                builder.setDescription("Something went wrong while loading the track! My owner has already been notified. Please try again later.");
                sendEmbed(e, builder, true, 1, TimeUnit.MINUTES);

                if (logger.isErrorEnabled()) {
                    logger.error("Load failed for track: {}", link, exception);
                }
            }
        };
    }
}