package dev.zawarudo.holo.commands.image;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.exceptions.HttpStatusException;
import dev.zawarudo.holo.utils.exceptions.HttpTransportException;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Command to fetch a quote from inspirobot.me and display it in an embed.
 */
@CommandInfo(name = "inspiro",
    description = "Fetch a random quote from [InspiroBot](https://inspirobot.me).",
    thumbnail = "https://inspirobot.me/website/images/inspirobot-dark-green.png",
    embedColor = EmbedColor.INSPIRO,
    guildOnly = false,
    category = CommandCategory.IMAGE)
public class InspiroCmd implements CommandMetadata, ExecutableCommand {

    private static final String API_URL = "https://inspirobot.me/api?generate=true";

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        String imageUrl;
        try {
            imageUrl = HoloHttp.readLine(API_URL);
        } catch (HttpStatusException ex) {
            ctx.reply().errorEmbed("InspiroBot returned an error (HTTP " + ex.getStatusCode() + "). Please try again later.");
            return;
        } catch (HttpTransportException ex) {
            ctx.reply().errorEmbed("I couldn't reach InspiroBot. Please try again in a few minutes.");
            return;
        }

        if (imageUrl.isBlank()) {
            ctx.reply().errorEmbed("InspiroBot returned an empty response. Please try again.");
            return;
        }

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("InspiroBot Quote")
            .setImage(imageUrl)
            .setColor(getEmbedColor());

        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getAvatarUrl()));
        ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(5, TimeUnit.MINUTES, null, ignored -> {
        }));
    }
}
