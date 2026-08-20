package dev.zawarudo.holo.commands.music;

import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.music.GuildMusicManager;
import dev.zawarudo.holo.modules.music.PlayerManager;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@CommandInfo(name = "stop",
    description = "Stops the current song and clears the queue.",
    ownerOnly = true,
    category = CommandCategory.MUSIC)
public class StopCmd extends AbstractMusicCommand implements ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.guild().orElseThrow());

        if (musicManager.scheduler.audioPlayer.getPlayingTrack() == null && musicManager.scheduler.queue.isEmpty()) {
            ctx.reply().errorEmbed("I'm currently idle!");
            return;
        }

        musicManager.clear();

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Success");
        builder.setDescription("Stopped current track and cleared queue!");
        ctx.reply().embed(builder.build(), 15, TimeUnit.SECONDS);
    }
}
