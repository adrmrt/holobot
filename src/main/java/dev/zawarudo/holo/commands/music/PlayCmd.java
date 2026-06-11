package dev.zawarudo.holo.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.music.GuildMusicManager;
import dev.zawarudo.holo.modules.music.PlayerManager;
import dev.zawarudo.holo.utils.EmbedUtils;
import dev.zawarudo.holo.utils.ParsingUtils;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "play",
    description = "Play a track or playlist from YouTube, SoundCloud, Bandcamp, Vimeo, Twitch, or a direct HTTP URL. Adds to the queue if something is already playing.",
    usage = "<url>",
    alias = {"p"},
    category = CommandCategory.MUSIC)
public class PlayCmd extends AbstractMusicCommand implements ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        Guild guild = ctx.guild().orElseThrow();
        Member member = ctx.member().orElseThrow();

        EmbedBuilder builder = new EmbedBuilder();
        builder.setFooter("Invoked by " + member.getEffectiveName(), ctx.user().getEffectiveAvatarUrl());
        builder.setColor(getEmbedColor());

        if (!isUserInAudioChannel(member)) {
            builder.setTitle("Not in a voice channel!");
            builder.setDescription("You need to be in a voice channel to use this command!");
            EmbedUtils.sendTimed(ctx.channel(), builder.build(), 15, TimeUnit.SECONDS);
            return;
        }

        if (isBotInAudioChannel(guild) && !isUserInSameAudioChannel(member, guild)) {
            builder.setTitle("Already playing elsewhere!");
            builder.setDescription("I'm already in " + Objects.requireNonNull(getConnectedChannel(guild)).getAsMention() + "!");
            EmbedUtils.sendTimed(ctx.channel(), builder.build(), 15, TimeUnit.SECONDS);
            return;
        }

        if (!ctx.hasArgs()) {
            builder.setTitle("Incorrect Usage");
            builder.setDescription("Please provide a link!");
            EmbedUtils.sendTimed(ctx.channel(), builder.build(), 15, TimeUnit.SECONDS);
            return;
        }

        String link = ctx.args().getFirst().replace("<", "").replace(">", "");

        if (!ParsingUtils.isValidUrl(link)) {
            builder.setTitle("Invalid Link");
            builder.setDescription("Please provide a valid link!");
            EmbedUtils.sendTimed(ctx.channel(), builder.build(), 15, TimeUnit.SECONDS);
            return;
        }

        // Join VC after explicitly validating link
        if (!isBotInAudioChannel(guild)) {
            AudioChannelUnion userChannel = getMemberVoiceState(member).getChannel();
            guild.getAudioManager().openAudioConnection(userChannel);
        }

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);
        AudioLoadResultHandler audioLoadResultHandler = getAudioLoadResultHandler(ctx.channel(), builder, link, musicManager);
        PlayerManager.getInstance().loadAndPlay(guild, link, audioLoadResultHandler);
    }

    /**
     * Creates an {@link AudioLoadResultHandler} object that handles the result of the audio loading and returns it.
     */
    private AudioLoadResultHandler getAudioLoadResultHandler(MessageChannelUnion channel, EmbedBuilder builder, String link, GuildMusicManager musicManager) {
        return new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.scheduler.enqueue(track);

                builder.setTitle("Added to the queue");
                String artworkUrl = getThumbnailUrl(track);
                if (artworkUrl != null) builder.setThumbnail(artworkUrl);
                builder.addField("Title", track.getInfo().title, false);
                builder.addField("Uploader", track.getInfo().author, false);
                builder.addField("Link", "[Open](" + link + ")", false);
                EmbedUtils.sendTimed(channel, builder.build(), 1, TimeUnit.MINUTES);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                musicManager.scheduler.enqueue(tracks);

                builder.setTitle("Added to the queue");
                builder.setDescription("`" + tracks.size() + "` tracks from playlist `" + playlist.getName() + "`");
                builder.addField("Link", "[Open](" + link + ")", false);
                EmbedUtils.sendTimed(channel, builder.build(), 1, TimeUnit.MINUTES);
            }

            @Override
            public void noMatches() {
                builder.setTitle("No matches!");
                builder.setDescription("I couldn't find any matches for the given link! Please make sure it's a valid link and try again.");
                EmbedUtils.sendTimed(channel, builder.build(), 1, TimeUnit.MINUTES);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                builder.setTitle("Load failed!");
                builder.setDescription("Something went wrong while loading the track! My owner has already been notified. Please try again later.");
                EmbedUtils.sendTimed(channel, builder.build(), 1, TimeUnit.MINUTES);

                if (logger.isErrorEnabled()) {
                    logger.error("Load failed for track: {}", link, exception);
                }
            }
        };
    }
}
