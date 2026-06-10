package dev.zawarudo.holo.commands.image;

import de.androidpit.colorthief.ColorThief;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.DateTimeUtils;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.ImageOperations;
import dev.zawarudo.holo.utils.ImageResolver;
import dev.zawarudo.holo.utils.ParsingUtils;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CommandInfo(name = "palette",
    description = "Creates a palette of representative colors for a given image.",
    usage = "[<color count>] [<image url>]",
    thumbnail = "https://www.pinclipart.com/picdir/big/141-1416768_painting-clipart-paint-palette-art-emoji-png-transparent.png",
    category = CommandCategory.IMAGE)
public class PaletteCmd extends AbstractCommand implements ExecutableCommand {

    private static final int DEFAULT_COLOR_COUNT = 5;
    private static final String PALETTE_IMAGE_FORMAT = "palette_%s.png";
    private static final int DEFAULT_BOX_SIZE = 200;

    private final ImageResolver imageResolver;

    public PaletteCmd(ImageResolver imageResolver) {
        this.imageResolver = imageResolver;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        Message msg = ctx.message().orElseThrow();
        Optional<String> url = msg.getReferencedMessage() == null
            ? imageResolver.resolveImageUrl(msg)
            : imageResolver.resolveImageUrl(msg.getReferencedMessage());

        if (url.isEmpty() || url.get().contains("gif")) {
            ctx.reply().errorEmbed("Incorrect usage! Please provide an image, either as an attachment or as an url.");
            return;
        }

        ctx.invocation().deleteInvokeIfPossible();
        String name = String.format(PALETTE_IMAGE_FORMAT, DateTimeUtils.getCurrentDateTimeString());

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Color Palette");
        builder.setThumbnail(url.get());
        builder.setImage("attachment://" + name);
        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl()));

        ColorResult result;
        InputStream imageStream;

        try {
            BufferedImage image = readImage(url.get());
            result = analyzeColors(image, parseColorCount(ctx));
            BufferedImage paletteImage = createPaletteImage(result.representativeColors);
            imageStream = ImageOperations.toInputStream(paletteImage);
        } catch (IOException | URISyntaxException e) {
            logger.error("Something went wrong while creating a color palette.", e);
            ctx.reply().errorEmbed("Something went wrong. Please try again later.");
            return;
        }

        builder.setColor(result.dominantColor);
        builder.setDescription(result.formatColors());
        FileUpload upload = FileUpload.fromData(imageStream, name);
        ctx.channel().sendFiles(upload).setEmbeds(builder.build()).queue();
    }

    private BufferedImage readImage(String url) throws URISyntaxException, IOException {
        return ImageIO.read(new URI(url).toURL());
    }

    private ColorResult analyzeColors(BufferedImage image, int count) {
        int[][] array = ColorThief.getPalette(image, count);
        List<Color> colors = convertToIntColorList(array);

        int[] dominantColor = ColorThief.getColor(image);
        Color dominant = convertToIntColorList(dominantColor).getFirst();

        return new ColorResult(dominant, colors);
    }

    private List<Color> convertToIntColorList(int[]... rgbArray) {
        return Arrays.stream(rgbArray)
            .filter(rgb -> rgb.length == 3)
            .map(rgb -> new Color(rgb[0], rgb[1], rgb[2]))
            .toList();
    }

    private BufferedImage createPaletteImage(List<Color> colors) {
        int stripeWidth = colors.size() > 10 ? DEFAULT_BOX_SIZE / 2 : DEFAULT_BOX_SIZE;
        int stripeHeight = DEFAULT_BOX_SIZE;

        BufferedImage image = new BufferedImage(stripeWidth * colors.size(), stripeHeight, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        for (int i = 0; i < colors.size(); i++) {
            g.setColor(colors.get(i));
            g.fillRect(i * stripeWidth, 0, stripeWidth, stripeHeight);
        }
        g.dispose();
        return image;
    }

    private int parseColorCount(CommandContext ctx) {
        if (ctx.hasArgs() && ParsingUtils.isInteger(ctx.args().getFirst())) {
            int count = Math.max(2, Integer.parseInt(ctx.args().getFirst()));
            return Math.min(count, 20);
        }
        return DEFAULT_COLOR_COUNT;
    }

    record ColorResult(Color dominantColor, List<Color> representativeColors) {
        public String formatColors() {
            return representativeColors.stream()
                .map(Formatter::getColorHexString)
                .map(hex -> "* " + hex)
                .collect(Collectors.joining("\n"));
        }
    }
}
