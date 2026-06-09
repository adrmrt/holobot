package dev.zawarudo.holo.commands.music;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.utils.Emote;
import dev.zawarudo.holo.modules.music.GuildMusicManager;
import dev.zawarudo.holo.modules.music.PlayerManager;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "skip",
    description = "Requests to skip the current track. About half of the members " +
        "in the voice channel (bot excluded) that are actively listening (i.e. " +
        "not deafened) have to react with an upvote in order to skip the current track.",
    category = CommandCategory.MUSIC)
public class SkipCmd extends AbstractMusicCommand {

    private final EventWaiter waiter;

    public SkipCmd(EventWaiter waiter) {
        this.waiter = waiter;
    }

    @Override
    public void onCommand(@NotNull MessageReceivedEvent event) {
        deleteInvoke(event);

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());

        // Checks if there are tracks to skip
        if (musicManager.audioPlayer.getPlayingTrack() == null) {
            sendErrorEmbed(event, "I'm not playing any tracks at the moment!");
            return;
        }

        // Bot owner can always skip
        if (isBotOwner(event.getAuthor())) {
            musicManager.resetVoting();
            skip(event);
            return;
        }

        // Check vc conditions (user and bot in same vc, etc.)
        if (!isUserInSameAudioChannel(event)) {
            sendErrorEmbed(event, "You need to be in the same voice channel as me to use this command!");
            return;
        }

        // Checks if there is already a voting for the guild
        if (musicManager.isVoting()) {
            sendErrorEmbed(event, "There is already a voting ongoing!");
            return;
        }

        AudioChannelUnion channel = getConnectedChannel(event.getGuild());

        if (channel == null) {
            sendErrorEmbed(event, "I am not connected to a voice channel!");
            return;
        }

        musicManager.setVoting(true);

        List<Member> listeners = channel.getMembers().stream()
            .filter(m -> !m.getUser().isBot() && !getMemberVoiceState(m).isDeafened()).toList();

        int requiredVotes = (int) Math.floor(listeners.size() / 2.0);

        // User can clear without voting
        if (requiredVotes == 0) {
            musicManager.resetVoting();
            skip(event);
            return;
        }

        String username = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(username + " requested a skip");
        builder.setDescription("Upvote to skip current track\n`" + requiredVotes + "` upvotes are required");

        event.getChannel().sendMessageEmbeds(builder.build()).queue(msg -> {
            msg.addReaction(Emote.ARROW_UP.getAsEmoji()).queue(v -> {
            }, err -> {
            });

            waiter.waitForEvent(MessageReactionAddEvent.class, evt -> {
                // So reactions on other messages and bot reactions are ignored
                if (evt.getMessageIdLong() != msg.getIdLong()) {
                    return false;
                }

                if (evt.retrieveUser().complete().isBot()) {
                    return false;
                }

                if (listeners.contains(evt.getMember()) && evt.getReaction().getEmoji().equals(Emote.ARROW_UP.getAsEmoji())) {
                    return musicManager.getVoteCounter().incrementAndGet() >= requiredVotes;
                }
                return false;
            }, evt -> {
                msg.delete().queue();
                musicManager.resetVoting();
                skip(event);
            }, 1L, TimeUnit.MINUTES, () -> {
                msg.delete().queue();
                musicManager.resetVoting();
            });
        });
    }

    private void skip(MessageReceivedEvent e) {
        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(e.getGuild());
        EmbedBuilder builder = new EmbedBuilder();
        musicManager.scheduler.playNext();
        builder.setTitle("Skipped Track");
        builder.setDescription(musicManager.audioPlayer.getPlayingTrack() == null ? "Nothing to play next!"
            : "Now playing: `" + musicManager.audioPlayer.getPlayingTrack().getInfo().title + "`");
        sendEmbed(e, builder, true, 30, TimeUnit.SECONDS);
    }
}
