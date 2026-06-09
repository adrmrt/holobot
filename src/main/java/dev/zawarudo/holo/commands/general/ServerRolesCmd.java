package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "serverroles",
    description = "Shows all the roles of the server",
    category = CommandCategory.GENERAL)
public class ServerRolesCmd extends AbstractCommand implements ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Roles of " + ctx.guild().orElseThrow().getName());
        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getAvatarUrl()));

        List<Role> roles = ctx.guild().orElseThrow().getRoles();

        if (roles.isEmpty()) {
            builder.setDescription("This server doesn't have any roles");
            ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES, null, ignored -> {
            }));
            return;
        }

        StringBuilder fieldContent = new StringBuilder();
        int counter = 0;

        for (Role role : roles) {
            String roleText = String.format("%s%n(%s)%n", role.getAsMention(), role.getId());

            if (fieldContent.length() + roleText.length() > MessageEmbed.VALUE_MAX_LENGTH) {
                builder.addField(Integer.toString(counter), fieldContent.toString(), true);
                fieldContent = new StringBuilder(roleText);
                counter++;
            } else {
                fieldContent.append(roleText);
            }
        }

        if (!fieldContent.isEmpty()) {
            builder.addField(Integer.toString(counter), fieldContent.toString(), true);
        }

        ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES, null, ignored -> {
        }));
    }
}
