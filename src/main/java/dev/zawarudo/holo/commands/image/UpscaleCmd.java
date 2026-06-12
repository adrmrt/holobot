package dev.zawarudo.holo.commands.image;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.ImageResolver;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CommandInfo(name = "upscale",
    description = "This command lets you Upscale a given image with Waifu2x. Please provide an image " +
        "as an attachment or as a link to process it. Alternatively, you can reply to a message " +
        "with an image.",
    category = CommandCategory.IMAGE)
public class UpscaleCmd implements CommandMetadata, ExecutableCommand {

    /**
     * The URL of the Waifu2x API.
     */
    public static final String API_URL = "https://api.deepai.org/api/waifu2x";

    private final ImageResolver imageResolver;

    public UpscaleCmd(ImageResolver imageResolver) {
        this.imageResolver = imageResolver;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        Message referenced = ctx.message().map(Message::getReferencedMessage).orElse(null);
        Optional<String> url = referenced != null
            ? imageResolver.resolveImageUrl(referenced)
            : imageResolver.resolveImageUrl(ctx.message().orElseThrow());

        if (url.isEmpty()) {
            ctx.reply().errorEmbed("You need to provide an image to upscale!");
            return;
        }

        ctx.reply().typing();

        String imageUrl;
        try {
            imageUrl = process(url.get());
        } catch (IOException _) {
            ctx.reply().errorEmbed("Something went wrong while processing your image! Please make sure it's an image and try again.");
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Upscaled Image");
        embedBuilder.setImage(imageUrl);
        embedBuilder.setColor(getEmbedColor());
        ctx.channel().sendMessageEmbeds(embedBuilder.build()).queue(msg -> msg.delete().queueAfter(5, TimeUnit.MINUTES, null, ignored -> {
        }));
    }

    /**
     * Sends a given image URL to the Waifu2x API where it is upscaled. The processed image is then returned.
     *
     * @param url The URL of the image to upscale.
     * @return The URL of the upscaled image.
     */
    public static String process(String url) throws IOException {
        String token = Bootstrap.holo.getConfig().getDeepAIKey();
        ProcessBuilder processBuilder = new ProcessBuilder(
            "curl",
            "-F",
            "image=" + url,
            "-H",
            "api-key:" + token,
            API_URL
        );
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String result = reader.lines().collect(Collectors.joining("\n"));
        JsonObject obj = JsonParser.parseString(result).getAsJsonObject();
        if (obj == null || obj.get("err") != null) {
            throw new IOException("No result!");
        }
        return obj.get("output_url").getAsString();
    }
}
