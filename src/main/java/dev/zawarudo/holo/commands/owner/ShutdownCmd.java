package dev.zawarudo.holo.commands.owner;

import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.commands.CommandCategory;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import org.jetbrains.annotations.NotNull;

@CommandInfo(name = "shutdown",
    description = "Shuts down the bot.",
    alias = {"kill"},
    ownerOnly = true,
    category = CommandCategory.OWNER)
public class ShutdownCmd implements CommandMetadata, ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.reply().text("Shutting down... Goodbye!");

        ctx.jda().getGuilds().stream().filter(g -> {
            GuildVoiceState self = g.getSelfMember().getVoiceState();
            if (self == null) {
                return false;
            }
            return self.inAudioChannel();
        }).forEach(g -> g.getAudioManager().closeAudioConnection());

        Bootstrap.holo.getPokemonSpawnManager().getMessages().values().forEach(m -> m.delete().queue());

        Bootstrap.shutdown();
    }
}
