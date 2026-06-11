package dev.zawarudo.holo.commands.music;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.music.GuildMusicManager;
import dev.zawarudo.holo.modules.music.PlayerManager;
import dev.zawarudo.holo.utils.EmbedUtils;
import dev.zawarudo.holo.utils.Emote;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "clear",
    description = "Requests to clear the queue. About half of the members in the voice channel (bot excluded) " +
        "that are actively listening (i.e. not deafened) have to react with an upvote in order to clear the queue.",
    category = CommandCategory.MUSIC)
public class ClearCmd extends AbstractMusicCommand implements ExecutableCommand {

    private final EventWaiter waiter;

    public ClearCmd(EventWaiter waiter) {
        this.waiter = waiter;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        Guild guild = ctx.guild().orElseThrow();
        Member member = ctx.member().orElseThrow();
        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);

        // Checks if queue is empty
        if (musicManager.scheduler.queue.isEmpty()) {
            ctx.reply().errorEmbed("My queue is already empty!");
            return;
        }

        MessageChannelUnion channel = ctx.channel();
        Color embedColor = getEmbedColor();
        String footerText = "Invoked by " + member.getEffectiveName();
        String footerIconUrl = ctx.user().getEffectiveAvatarUrl();

        // Owner can always clear
        if (ctx.isBotOwner()) {
            musicManager.resetVoting();
            sendClearedEmbed(channel, musicManager, embedColor, footerText, footerIconUrl);
            return;
        }

        // Checks vc conditions (user and bot in same vc, etc.)
        if (!isUserInSameAudioChannel(member, guild)) {
            ctx.reply().errorEmbed("You need to be in the same voice channel as me to use this command!");
            return;
        }

        // Checks if there is already a voting for the guild
        if (musicManager.isVoting()) {
            ctx.reply().errorEmbed("There is already a voting ongoing!");
            return;
        }

        musicManager.setVoting(true);

        AudioChannelUnion voiceChannel = getConnectedChannel(guild);

        if (voiceChannel == null) {
            ctx.reply().errorEmbed("I am not connected to a voice channel!");
            return;
        }

        List<Member> listeners = voiceChannel.getMembers().stream()
            .filter(m -> !m.getUser().isBot() && !getMemberVoiceState(m).isDeafened()).toList();

        int requiredVotes = (int) Math.floor(listeners.size() / 2.0);

        // User can clear without voting
        if (requiredVotes == 0) {
            musicManager.resetVoting();
            sendClearedEmbed(channel, musicManager, embedColor, footerText, footerIconUrl);
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(member.getEffectiveName() + " requested to clear the queue");
        builder.setDescription("Upvote to clear the queue\n`" + requiredVotes + "` upvotes are required");
        builder.setColor(embedColor);

        channel.sendMessageEmbeds(builder.build()).queue(msg -> {
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
                sendClearedEmbed(channel, musicManager, embedColor, footerText, footerIconUrl);
            }, 1L, TimeUnit.MINUTES, () -> {
                msg.delete().queue();
                musicManager.resetVoting();
            });
        });
    }

    private void sendClearedEmbed(MessageChannelUnion channel, GuildMusicManager musicManager, Color embedColor, String footerText, String footerIconUrl) {
        musicManager.clear();

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Queue Cleared");
        builder.setDescription("My queue is now empty");
        builder.setColor(embedColor);
        builder.setFooter(footerText, footerIconUrl);

        EmbedUtils.sendTimed(channel, builder.build(), 30, TimeUnit.SECONDS);
    }
}
