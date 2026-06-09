package dev.zawarudo.holo.commands.image;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.UserResolver;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "avatar",
    description = "Retrieves the avatar of a specified user. Tip: Use the id of the user if you don't want to ping them.",
    usage = "[<user id>]",
    alias = {"av", "pfp"},
    category = CommandCategory.IMAGE)
public class AvatarCmd extends AbstractCommand implements ExecutableCommand {

    private final UserResolver userResolver;

    public AvatarCmd(UserResolver userResolver) {
        this.userResolver = userResolver;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        Optional<User> userOptional = userResolver.resolveUser(ctx);
        if (userOptional.isEmpty()) {
            ctx.reply().errorEmbed("I couldn't find the given user! Please make sure you provided the correct user id or mentioned them!");
            return;
        }
        User user = userOptional.get();

        Optional<Member> member = userResolver.resolveGuildMember(user, ctx);

        String name = member.map(Member::getEffectiveName).orElseGet(user::getName);
        String userAvatar = user.getEffectiveAvatarUrl() + "?size=1024";
        String serverAvatar = member.map(m -> m.getEffectiveAvatarUrl() + "?size=1024").orElse(null);
        Color embedColor = member.map(m -> m.getColors().getPrimary()).orElse(null);

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Avatar of " + name, userAvatar);
        builder.setImage(userAvatar);

        if (serverAvatar != null && !serverAvatar.equals(userAvatar)) {
            List<MessageEmbed> embeds = multiImageEmbed(builder, userAvatar, serverAvatar);
            ctx.channel().sendMessageEmbeds(embeds).queue(msg -> msg.delete().queueAfter(5, TimeUnit.MINUTES));
        } else {
            builder.setColor(embedColor);
            ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getAvatarUrl()));
            ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(5, TimeUnit.MINUTES, null, ignored -> {
            }));
        }
    }

    private static List<MessageEmbed> multiImageEmbed(EmbedBuilder builder, String... images) {
        List<MessageEmbed> embeds = new ArrayList<>();
        for (String image : images) {
            builder.setImage(image);
            embeds.add(builder.build());
        }
        return embeds;
    }
}
