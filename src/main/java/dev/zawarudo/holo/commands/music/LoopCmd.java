package dev.zawarudo.holo.commands.music;

import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.music.GuildMusicManager;
import dev.zawarudo.holo.modules.music.PlayerManager;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.annotations.Deactivated;
import org.jetbrains.annotations.NotNull;

// TODO: Fully implement the command

@Deactivated
@CommandInfo(name = "loop",
    description = "Loops the current song",
    category = CommandCategory.MUSIC)
public class LoopCmd extends AbstractMusicCommand implements ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.guild().orElseThrow());
        boolean repeating = !musicManager.scheduler.looping;
        musicManager.scheduler.looping = repeating;
        ctx.channel().sendMessageFormat("Loop %s", repeating ? "enabled" : "disabled").queue();
    }
}
