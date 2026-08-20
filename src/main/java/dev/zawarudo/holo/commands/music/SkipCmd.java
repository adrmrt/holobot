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

@CommandInfo(name = "skip",
    description = "Requests to skip the current track. About half of the members " +
        "in the voice channel (bot excluded) that are actively listening (i.e. " +
        "not deafened) have to react with an upvote in order to skip the current track.",
    category = CommandCategory.MUSIC)
public class SkipCmd extends AbstractMusicCommand implements ExecutableCommand {

    private final EventWaiter waiter;

    public SkipCmd(EventWaiter waiter) {
        this.waiter = waiter;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        Guild guild = ctx.guild().orElseThrow();
        Member member = ctx.member().orElseThrow();
        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);

        // Checks if there are tracks to skip
        if (musicManager.audioPlayer.getPlayingTrack() == null) {
            ctx.reply().errorEmbed("I'm not playing any tracks at the moment!");
            return;
        }

        MessageChannelUnion channel = ctx.channel();
        Color embedColor = getEmbedColor();
        String footerText = "Invoked by " + member.getEffectiveName();
        String footerIconUrl = ctx.user().getEffectiveAvatarUrl();

        // Bot owner can always skip
        if (ctx.isBotOwner()) {
            musicManager.resetVoting();
            sendSkippedEmbed(channel, musicManager, embedColor, footerText, footerIconUrl);
            return;
        }

        // Check vc conditions (user and bot in same vc, etc.)
        if (!isUserInSameAudioChannel(member, guild)) {
            ctx.reply().errorEmbed("You need to be in the same voice channel as me to use this command!");
            return;
        }

        // Checks if there is already a voting for the guild
        if (musicManager.isVoting()) {
            ctx.reply().errorEmbed("There is already a voting ongoing!");
            return;
        }

        AudioChannelUnion voiceChannel = getConnectedChannel(guild);

        if (voiceChannel == null) {
            ctx.reply().errorEmbed("I am not connected to a voice channel!");
            return;
        }

        musicManager.setVoting(true);

        List<Member> listeners = voiceChannel.getMembers().stream()
            .filter(m -> !m.getUser().isBot() && !getMemberVoiceState(m).isDeafened()).toList();

        int requiredVotes = (int) Math.floor(listeners.size() / 2.0);

        // User can skip without voting
        if (requiredVotes == 0) {
            musicManager.resetVoting();
            sendSkippedEmbed(channel, musicManager, embedColor, footerText, footerIconUrl);
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(member.getEffectiveName() + " requested a skip");
        builder.setDescription("Upvote to skip current track\n`" + requiredVotes + "` upvotes are required");
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
                sendSkippedEmbed(channel, musicManager, embedColor, footerText, footerIconUrl);
            }, 1L, TimeUnit.MINUTES, () -> {
                msg.delete().queue();
                musicManager.resetVoting();
            });
        });
    }

    private void sendSkippedEmbed(MessageChannelUnion channel, GuildMusicManager musicManager, Color embedColor, String footerText, String footerIconUrl) {
        musicManager.scheduler.playNext();

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Skipped Track");
        builder.setDescription(musicManager.audioPlayer.getPlayingTrack() == null ? "Nothing to play next!"
            : "Now playing: `" + musicManager.audioPlayer.getPlayingTrack().getInfo().title + "`");
        builder.setColor(embedColor);
        builder.setFooter(footerText, footerIconUrl);

        EmbedUtils.sendTimed(channel, builder.build(), 30, TimeUnit.SECONDS);
    }
}
