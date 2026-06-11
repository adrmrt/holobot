package dev.zawarudo.holo.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.music.GuildMusicManager;
import dev.zawarudo.holo.modules.music.PlayerManager;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@CommandInfo(name = "clone",
    description = "Duplicates the currently playing track and adds it on top of the queue.",
    category = CommandCategory.MUSIC)
public class CloneCmd extends AbstractMusicCommand implements ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.guild().orElseThrow());

        AudioTrack current = musicManager.audioPlayer.getPlayingTrack();

        // Check if there are any tracks playing
        if (current == null) {
            ctx.reply().errorEmbed("I'm not playing any tracks at the moment!");
            return;
        }

        // Check vc conditions (user and bot in same vc, etc.)
        if (!isUserInSameAudioChannel(ctx.member().orElseThrow(), ctx.guild().orElseThrow())) {
            ctx.reply().errorEmbed("You need to be in the same voice channel as me to use this command!");
            return;
        }

        musicManager.scheduler.enqueueFirst(current.makeClone());

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Cloned track");
        String artworkUrl = getThumbnailUrl(current);
        if (artworkUrl != null) builder.setThumbnail(artworkUrl);
        builder.addField("Title", current.getInfo().title, false);
        builder.addField("Uploader", current.getInfo().author, false);
        builder.addField("Link", "[Open](" + current.getInfo().uri + ")", false);
        builder.setColor(getEmbedColor());

        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl()));

        ctx.reply().embed(builder.build(), 1, TimeUnit.MINUTES);
    }
}
