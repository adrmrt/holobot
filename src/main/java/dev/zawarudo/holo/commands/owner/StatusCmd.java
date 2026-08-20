package dev.zawarudo.holo.commands.owner;

import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Activity;
import org.jetbrains.annotations.NotNull;

/**
 * Command to set the status of the bot
 */
@CommandInfo(name = "status",
    description = "Sets the status of the bot.",
    usage = "[default | listening <message> | watching <message> | playing <message> | competing <message>]",
    ownerOnly = true,
    category = CommandCategory.OWNER)
public class StatusCmd implements CommandMetadata, ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        if (!ctx.hasArgs()) {
            int guilds = ctx.jda().getGuilds().size();
            int users = ctx.jda().getUsers().size();
            ctx.jda().getPresence().setActivity(Activity.listening(users + " users on " + guilds + " servers"));
            return;
        }

        if (ctx.args().getFirst().equals("default")) {
            ctx.jda().getPresence().setActivity(Activity.watching(ctx.prefix().orElse("") + "help"));
            return;
        }

        String status = String.join(" ", ctx.args().subList(1, ctx.args().size()));

        switch (ctx.args().getFirst()) {
            case "listening" -> setActivity(Activity.listening(status));
            case "playing" -> setActivity(Activity.playing(status));
            case "watching" -> setActivity(Activity.watching(status));
            case "competing" -> setActivity(Activity.competing(status));
            default -> {
            }
        }
    }

    private void setActivity(Activity activity) {
        Bootstrap.holo.getJDA().getPresence().setActivity(activity);
    }
}
