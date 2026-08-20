package dev.zawarudo.holo.commands.image;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@CommandInfo(name = "banner",
    description = "Retrieves the current banner of this guild. Note that a guild " +
        "needs to be boosted to level 2 in order to have a banner.",
    category = CommandCategory.IMAGE)
public class BannerCmd implements CommandMetadata, ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        Guild guild = ctx.guild().orElseThrow();
        EmbedBuilder builder = new EmbedBuilder();
        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getAvatarUrl()));

        String bannerUrl = guild.getBannerUrl();

        if (bannerUrl == null) {
            builder.setTitle("No Banner Found!");
            builder.setDescription("This guild doesn't seem to have a banner.");
            ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(15, TimeUnit.SECONDS, null, ignored -> {
            }));
        } else {
            builder.setTitle("Banner of " + guild.getName(), bannerUrl + "?size=4096");
            builder.setImage(bannerUrl + "?size=4096");
            ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES, null, ignored -> {
            }));
        }
    }
}
