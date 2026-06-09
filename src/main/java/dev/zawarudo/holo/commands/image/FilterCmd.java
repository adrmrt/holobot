package dev.zawarudo.holo.commands.image;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.image.FilterRegistry;
import dev.zawarudo.holo.modules.image.ImageFilter;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.ImageOperations;
import dev.zawarudo.holo.utils.ImageResolver;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CommandInfo(
    name = "filter",
    description = "Applies a dramatic grayscale filter while preserving red tones",
    alias = {"acheron"},
    category = CommandCategory.IMAGE,
    ownerOnly = true
)
public class FilterCmd extends AbstractCommand implements ExecutableCommand {

    private final ImageResolver imageResolver;

    public FilterCmd(ImageResolver imageResolver) {
        this.imageResolver = imageResolver;
    }

    @Override
    public void execute(@NonNull CommandContext ctx) {
        final List<String> args = ctx.args();

        // List / no args
        if (args.isEmpty() || "list".equalsIgnoreCase(args.getFirst())) {
            replyWithFilterListEmbed(ctx);
            return;
        }

        // This command needs a message to read attachments / referenced message.
        final Message invokeMsg = ctx.message().orElse(null);
        if (invokeMsg == null) {
            ctx.reply().errorEmbed("This command currently only works for message-based invocation.");
            return;
        }

        final Optional<String> imageUrl = resolveImageUrl(invokeMsg);
        if (imageUrl.isEmpty()) {
            ctx.reply().errorEmbed("You need to provide an image!");
            return;
        }

        final String filterName = args.getFirst();
        final ImageFilter filter = FilterRegistry.get(filterName).orElse(null);
        if (filter == null) {
            ctx.reply().errorEmbed("Unknown filter: `" + filterName + "`");
            return;
        }

        final String[] filterArgs = (args.size() > 1)
            ? args.subList(1, args.size()).toArray(String[]::new)
            : new String[0];

        ctx.reply().typing();
        applyFilterAndReply(invokeMsg, ctx, imageUrl.get(), filter, filterArgs);
    }

    private void replyWithFilterListEmbed(@NotNull CommandContext ctx) {
        EmbedBuilder eb = new EmbedBuilder()
            .setTitle("Available filters")
            .setDescription(formatFiltersAsCodeBlock());

        ctx.reply().embed(eb);
    }

    private Optional<String> resolveImageUrl(@NotNull Message invokeMessage) {
        Message referenced = invokeMessage.getReferencedMessage();
        return referenced != null
            ? imageResolver.resolveImageUrl(referenced)
            : imageResolver.resolveImageUrl(invokeMessage);
    }

    private void applyFilterAndReply(
        @NotNull Message invokeMsg,
        @NotNull CommandContext ctx,
        @NotNull String url,
        @NotNull ImageFilter filter,
        @NotNull String[] filterArgs
    ) {
        try {
            BufferedImage img = readImage(url);
            if (img == null) {
                ctx.reply().errorEmbed("I couldn't read the image. Please try a different format.");
                logger.error("Image is null: {}", url);
                return;
            }

            BufferedImage result = filter.apply(img, filterArgs);

            try (InputStream input = ImageOperations.toInputStream(result)) {
                invokeMsg.replyFiles(FileUpload.fromData(input, String.format("filter-%s.png", filter.name())))
                    .queue();
            }

        } catch (IllegalArgumentException ex) {
            ctx.reply().errorEmbed(ex.getMessage());
        } catch (IOException ex) {
            ctx.reply().errorEmbed("Something went wrong while applying the filter.");
            logger.error("Error applying filter {} on image: {}", filter.name(), url, ex);
        }
    }

    private BufferedImage readImage(String url) throws IOException {
        return ImageIO.read(URI.create(url).toURL());
    }

    private String formatFiltersAsCodeBlock() {
        List<ImageFilter> filters = FilterRegistry.list();

        String joined = filters.stream()
            .map(ImageFilter::name)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.joining("\n"));

        return Formatter.asCodeBlock(joined);
    }
}
