package dev.zawarudo.holo.commands.music;

import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.music.GuildMusicManager;
import dev.zawarudo.holo.modules.music.PlayerManager;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@CommandInfo(name = "shuffle",
    description = "Shuffles the current queue.",
    category = CommandCategory.MUSIC)
public class ShuffleCmd extends AbstractMusicCommand implements ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.guild().orElseThrow());

        if (musicManager.scheduler.queue.isEmpty()) {
            ctx.reply().errorEmbed("I can't shuffle an empty queue!");
            return;
        }

        musicManager.scheduler.shuffle();

        String userName = ctx.member().map(Member::getEffectiveName).orElseGet(() -> ctx.user().getName());

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Shuffled Queue");
        builder.setDescription(userName + " shuffled the queue!");
        ctx.reply().embed(builder.build(), 1, TimeUnit.MINUTES);
    }
}
