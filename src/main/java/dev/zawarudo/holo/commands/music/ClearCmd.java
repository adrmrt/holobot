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

@CommandInfo(name = "clear",
    description = "Requests to clear the queue. About half of the members in the voice channel (bot excluded) " +
        "that are actively listening (i.e. not deafened) have to react with an upvote in order to clear the queue.",
    category = CommandCategory.MUSIC)
public class ClearCmd extends AbstractMusicCommand {

    private final EventWaiter waiter;

    public ClearCmd(EventWaiter waiter) {
        this.waiter = waiter;
    }

    @Override
    public void onCommand(@NotNull MessageReceivedEvent event) {
        deleteInvoke(event);

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());

        // Checks if queue is empty
        if (musicManager.scheduler.queue.isEmpty()) {
            sendErrorEmbed(event, "My queue is already empty!");
            return;
        }

        // Owner can always clear
        if (isBotOwner(event.getAuthor())) {
            musicManager.resetVoting();
            clear(event);
            return;
        }

        // Checks vc conditions (user and bot in same vc, etc.)
        if (!isUserInSameAudioChannel(event)) {
            sendErrorEmbed(event, "You need to be in the same voice channel as me to use this command!");
            return;
        }

        // Checks if there is already a voting for the guild
        if (musicManager.isVoting()) {
            sendErrorEmbed(event, "There is already a voting ongoing!");
            return;
        }

        musicManager.setVoting(true);

        AudioChannelUnion channel = getConnectedChannel(event.getGuild());

        if (channel == null) {
            sendErrorEmbed(event, "I am not connected to a voice channel!");
            return;
        }

        List<Member> listeners = channel.getMembers().stream()
            .filter(m -> !m.getUser().isBot() && !getMemberVoiceState(m).isDeafened()).toList();

        int requiredVotes = (int) Math.floor(listeners.size() / 2.0);

        // User can clear without voting
        if (requiredVotes == 0) {
            musicManager.resetVoting();
            clear(event);
            return;
        }

        String username = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(username + " requested to clear the queue");
        builder.setDescription("Upvote to clear the queue\n`" + requiredVotes + "` upvotes are required");

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
                clear(event);
            }, 1L, TimeUnit.MINUTES, () -> {
                msg.delete().queue();
                musicManager.resetVoting();
            });
        });
    }

    private void clear(MessageReceivedEvent event) {
        PlayerManager.getInstance().getMusicManager(event.getGuild()).clear();

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Queue Cleared");
        builder.setDescription("My queue is now empty");
        sendEmbed(event, builder, true, 30, TimeUnit.SECONDS);
    }
}
