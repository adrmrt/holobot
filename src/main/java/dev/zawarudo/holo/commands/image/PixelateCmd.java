package dev.zawarudo.holo.commands.image;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.ImageOperations;
import dev.zawarudo.holo.utils.ImageResolver;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.annotations.Deactivated;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

@Deactivated
@CommandInfo(name = "pixelate",
    description = "Pixelates a given image",
    usage = "[<intensity>]",
    alias = {"pixel"},
    category = CommandCategory.IMAGE)
public class PixelateCmd extends AbstractCommand implements ExecutableCommand {

    private final ImageResolver imageResolver;

    public PixelateCmd(ImageResolver imageResolver) {
        this.imageResolver = imageResolver;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        Message msg = ctx.message().orElseThrow();
        Message referenced = msg.getReferencedMessage();
        Optional<String> url = referenced != null
            ? imageResolver.resolveImageUrl(referenced)
            : imageResolver.resolveImageUrl(msg);

        if (url.isEmpty()) {
            ctx.reply().errorEmbed("You need to provide an image to pixelate!");
            return;
        }

        int intensity = 1;

        if (ctx.hasArgs() && isInteger(ctx.args().getFirst())) {
            intensity = Integer.parseInt(ctx.args().getFirst());
            if (intensity < 1 || intensity > 250) {
                ctx.reply().errorEmbed("Intensity should be an integer between 1 and 250!");
                return;
            }
        }

        try {
            BufferedImage img = ImageIO.read(URI.create(url.get()).toURL());
            if (img == null) {
                ctx.reply().errorEmbed("I couldn't read the image. Please check your image format or try a different image.");
                logger.error("Image is null: {}", url);
                return;
            }
            BufferedImage result = pixelate(img, intensity);
            InputStream input = ImageOperations.toInputStream(result);
            msg.replyFiles(FileUpload.fromData(input, "result.png")).queue();
        } catch (IOException ex) {
            ctx.reply().errorEmbed("Something went wrong while pixelating your image. Please try again later.");
            logger.error("Something went wrong during the pixelation of the image: {}", url, ex);
        }
    }

    private BufferedImage pixelate(@NotNull BufferedImage img, int intensity) {
        int width = img.getWidth();
        int height = img.getHeight();

        int newWidth = (int) Math.ceil(width / (intensity * 4.0));
        int newHeight = (int) Math.ceil(height / (intensity * 4.0));

        BufferedImage result = ImageOperations.resize(img, newWidth, newHeight);
        return ImageOperations.resize(result, width, height);
    }
}
