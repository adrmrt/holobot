package dev.zawarudo.holo.commands.music;

import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.modules.music.GuildMusicManager;
import dev.zawarudo.holo.modules.music.PlayerManager;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.annotations.Deactivated;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

// TODO: Fully implement the command

@Deactivated
@CommandInfo(name = "loop",
    description = "Loops the current song",
    category = CommandCategory.MUSIC)
public class LoopCmd extends AbstractMusicCommand {

    @Override
    public void onCommand(@NotNull MessageReceivedEvent e) {
        deleteInvoke(e);

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(e.getGuild());
        boolean repeating = !musicManager.scheduler.looping;
        musicManager.scheduler.looping = repeating;
        e.getChannel().sendMessageFormat("Loop %s", repeating ? "enabled" : "disabled").queue();
    }
}
