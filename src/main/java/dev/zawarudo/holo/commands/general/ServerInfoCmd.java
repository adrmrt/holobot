package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "serverinfo",
    description = "Shows information about the server",
    category = CommandCategory.GENERAL)
public class ServerInfoCmd extends AbstractCommand implements ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        Guild guild = ctx.guild().orElseThrow();

        String creationDate = "`" + guild.getTimeCreated().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)) + "`";
        long normalCount = guild.getEmojis().stream().filter(em -> !em.isAnimated()).count();
        long animatedCount = guild.getEmojis().stream().filter(CustomEmoji::isAnimated).count();

        int stickerCount = guild.getStickers().size();
        int maxStickers = switch (guild.getBoostTier().getKey()) {
            case 1 -> 15;
            case 2 -> 30;
            case 3 -> 60;
            default -> 0;
        };

        String additionalChecks = "Normal Emotes: `" + normalCount + " / " + guild.getMaxEmojis() + "`\n"
            + "Animated Emotes: `" + animatedCount + " / " + guild.getMaxEmojis() + "`\n"
            + "Stickers: `" + stickerCount + " / " + maxStickers + "`\n"
            + "Channels: `" + guild.getChannels().size() + " / 500`\n"
            + "Roles: `" + guild.getRoles().size() + " / 250`";

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(guild.getName() + " (" + guild.getId() + ")");
        if (guild.getIconUrl() != null) {
            builder.setThumbnail(guild.getIconUrl());
        }
        builder.addField("Owner", guild.retrieveOwner().complete().getAsMention(), true);
        builder.addField("Members", String.valueOf(guild.getMemberCount()), true);
        builder.addField("Boost Level", String.valueOf(guild.getBoostTier().getKey()), false);
        builder.addField("Creation Date", creationDate, false);
        builder.addField("Additional Checks", additionalChecks, false);
        if (guild.getSplashUrl() != null) {
            builder.setImage(guild.getSplashUrl().replace(".png", ".webp") + "?size=4096");
        }

        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getAvatarUrl()));
        ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(5, TimeUnit.MINUTES, null, ignored -> {
        }));
    }
}
