package dev.zawarudo.holo.commands.owner;

import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.commands.CommandCategory;
import org.jetbrains.annotations.NotNull;

@CommandInfo(name = "restart",
    description = "Restarts the bot",
    alias = {"reboot"},
    ownerOnly = true,
    category = CommandCategory.OWNER)
public class RestartCmd implements CommandMetadata, ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.reply().text("Restarting... See you again in a few seconds!");

        ctx.jda().getGuilds().stream().filter(g -> {
            if (g.getSelfMember().getVoiceState() == null) {
                return false;
            }
            return g.getSelfMember().getVoiceState().inAudioChannel();
        }).forEach(g -> g.getAudioManager().closeAudioConnection());

        //Bootstrap.holo.getPokemonSpawnManager().getMessages().values().forEach(m -> m.delete().queue());

        ctx.jda().shutdown();
        Bootstrap.restart();
    }
}
