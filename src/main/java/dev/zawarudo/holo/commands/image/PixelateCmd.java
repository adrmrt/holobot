package dev.zawarudo.holo.commands.image;

import dev.zawarudo.holo.utils.ImageResolver;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.annotations.Deactivated;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.utils.ImageOperations;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
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
public class PixelateCmd extends AbstractCommand {

    private final ImageResolver imageResolver;

    public PixelateCmd(ImageResolver imageResolver) {
        this.imageResolver = imageResolver;
    }

    @Override
    public void onCommand(@NotNull MessageReceivedEvent event) {
        Message referenced = event.getMessage().getReferencedMessage();
        Optional<String> url = referenced != null
                ? imageResolver.resolveImageUrl(referenced)
                : imageResolver.resolveImageUrl(event.getMessage());

        // User didn't provide an image
        if (url.isEmpty()) {
            sendErrorEmbed(event, "You need to provide an image to pixelate!");
            return;
        }

        int intensity = 1;

        if (args.length > 0 && isInteger(args[0])) {
            intensity = Integer.parseInt(args[0]);
            if (intensity < 1 || intensity > 250) {
                sendErrorEmbed(event, "Intensity should be an integer between 1 and 250!");
                return;
            }
        }

        try {
            BufferedImage img = ImageIO.read(URI.create(url.get()).toURL());
            if (img == null) {
                sendErrorEmbed(event, "I couldn't read the image. Please check your image format or try a different image.");
                if (logger.isErrorEnabled()) {
                    logger.error("Image is null: {}", url);
                }
                return;
            }
            BufferedImage result = pixelate(img, intensity);
            InputStream input = ImageOperations.toInputStream(result);
            event.getMessage().replyFiles(FileUpload.fromData(input, "result.png")).queue();
        } catch (IOException ex) {
            sendErrorEmbed(event, "Something went wrong while pixelating your image. Please try again later.");
            if (logger.isErrorEnabled()) {
                logger.error("Something went wrong during the pixelation of the image: {}", url, ex);
            }
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