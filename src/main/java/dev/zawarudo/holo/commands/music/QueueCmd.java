package dev.zawarudo.holo.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.music.GuildMusicManager;
import dev.zawarudo.holo.modules.music.PlayerManager;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "queue",
    description = "Shows the current queue. You can also use `history` as an additional argument to view the last 10 tracks that was played.",
    usage = "[history]",
    alias = {"q"},
    category = CommandCategory.MUSIC)
public class QueueCmd extends AbstractMusicCommand implements ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        if (ctx.hasArgs() && "history".equals(ctx.args().getFirst())) {
            displayHistory(ctx);
        } else {
            displayQueue(ctx);
        }
    }

    /**
     * Display current queue.
     */
    private void displayQueue(CommandContext ctx) {
        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.guild().orElseThrow());
        BlockingQueue<AudioTrack> queue = musicManager.scheduler.queue;

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Queue");
        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl()));

        if (queue.isEmpty()) {
            builder.setDescription("My queue is empty!");
            ctx.reply().embed(builder.build(), 30, TimeUnit.SECONDS);
            return;
        }

        int trackCount = Math.min(queue.size(), 12);
        List<AudioTrack> trackList = new ArrayList<>(queue);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < trackCount; ++i) {
            AudioTrack track = trackList.get(i);
            AudioTrackInfo info = track.getInfo();
            sb.append(String.format("`#%02d %s by %s [%s]`", i + 1, info.title, info.author,
                Formatter.formatTrackTime(track.getDuration()))).append("\n");
        }

        if (trackList.size() > trackCount) {
            sb.append("And `").append(trackList.size() - trackCount).append("` more...");
        }

        // Get total duration of the queue + current track
        AudioTrack current = musicManager.audioPlayer.getPlayingTrack();
        long duration = current != null ? current.getDuration() - current.getPosition() : 0L;
        for (AudioTrack track : trackList) {
            duration += track.getDuration();
        }

        builder.setDescription(sb.toString());
        builder.addField("Total Duration", Formatter.formatTrackTime(duration), false);

        ctx.reply().embed(builder.build(), 1, TimeUnit.MINUTES);
    }

    /**
     * Display the last 10 tracks.
     */
    private void displayHistory(CommandContext ctx) {
        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.guild().orElseThrow());
        BlockingQueue<AudioTrack> history = musicManager.scheduler.history;

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Queue History");
        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl()));

        if (history == null || history.isEmpty()) {
            builder.setDescription("I didn't play any tracks recently!");
            ctx.reply().embed(builder.build(), 30, TimeUnit.SECONDS);
            return;
        }

        int trackCount = Math.min(history.size(), 10);
        List<AudioTrack> historyList = new ArrayList<>(history);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < trackCount; ++i) {
            AudioTrack track = historyList.get(i);
            AudioTrackInfo info = track.getInfo();
            sb.append(String.format("`#%02d %s [%s]` [%s]", i + 1, info.title,
                Formatter.formatTrackTime(track.getDuration()), "[link](" + info.uri + ")")).append("\n");
        }

        builder.setDescription(sb.toString());
        ctx.reply().embed(builder.build(), 1, TimeUnit.MINUTES);
    }
}
