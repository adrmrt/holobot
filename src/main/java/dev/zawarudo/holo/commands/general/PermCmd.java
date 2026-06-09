package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.annotations.Deactivated;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// TODO: Add argument to see guild-wide perms or only perms within a specific channel

@Deactivated
@CommandInfo(name = "perm",
    description = "Shows my permissions in this channel",
    category = CommandCategory.GENERAL)
public class PermCmd extends AbstractCommand implements ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        String permissionString = ctx.guild().orElseThrow().getSelfMember().getPermissions().stream()
            .map(Permission::getName)
            .collect(Collectors.joining("\n"));

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("My Permissions");
        builder.setDescription(Formatter.asCodeBlock(permissionString));
        ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES, null, ignored -> {
        }));
    }
}
