package dev.zawarudo.holo.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.zawarudo.holo.commands.AbstractCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class AbstractMusicCommand extends AbstractCommand {

    /**
     * Returns the voice state of a member within a guild.
     */
    @NotNull
    protected GuildVoiceState getMemberVoiceState(Member member) {
        return Objects.requireNonNull(member.getVoiceState());
    }

    /**
     * Checks if the bot is in a voice channel of the guild
     */
    protected boolean isBotInAudioChannel(Guild guild) {
        return guild.getAudioManager().isConnected();
    }

    /**
     * Returns the audio channel the bot is connected to.
     */
    @Nullable
    protected AudioChannelUnion getConnectedChannel(Guild guild) {
        return guild.getAudioManager().getConnectedChannel();
    }

    /**
     * Checks if the user who invoked the command is in a voice channel
     */
    protected boolean isUserInAudioChannel(Member member) {
        if (member.getVoiceState() == null) {
            return false;
        }
        return member.getVoiceState().inAudioChannel();
    }

    /**
     * Returns the thumbnail URL for a track. Uses artworkUrl when available; falls back
     * to a YouTube thumbnail derived from the video ID for YouTube tracks.
     */
    @Nullable
    protected String getThumbnailUrl(AudioTrack track) {
        if (track.getInfo().artworkUrl != null) {
            return track.getInfo().artworkUrl;
        }
        String[] parts = track.getInfo().uri.split("v=");
        if (parts.length > 1) {
            String videoId = parts[1].split("&")[0];
            return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
        }
        return null;
    }

    /**
     * Checks if the user and the bot are in the same voice channel
     */
    protected boolean isUserInSameAudioChannel(MessageReceivedEvent e) {
        if (e.getMember() == null) {
            return false;
        }
        return isUserInSameAudioChannel(e.getMember(), e.getGuild());
    }

    /**
     * Checks if the user and the bot are in the same voice channel
     */
    protected boolean isUserInSameAudioChannel(Member member, Guild guild) {
        if (!isUserInAudioChannel(member) || !isBotInAudioChannel(guild)) {
            return false;
        }

        // Check voice states
        AudioChannel botChannel = getConnectedChannel(guild);
        if (botChannel == null) {
            return false;
        }
        AudioChannel userChannel = getMemberVoiceState(member).getChannel();
        return botChannel.equals(userChannel);
    }
}
