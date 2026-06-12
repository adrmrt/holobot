package dev.zawarudo.holo.commands.image;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.TheCatApiClient;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "cat",
    description = "Fetches an image of a cat.",
    usage = "[breeds | <breed> | random]",
    category = CommandCategory.IMAGE)
public class CatCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatCmd.class);

    /** Maps lowercase breed name → breed record. Populated at construction. Empty on API failure. */
    private final Map<String, TheCatApiClient.CatBreed> breeds = new LinkedHashMap<>();

    public CatCmd() {
        try {
            for (TheCatApiClient.CatBreed breed : TheCatApiClient.getBreeds()) {
                breeds.put(breed.name().toLowerCase(), breed);
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to load cat breeds from The Cat API — breed filtering unavailable.", ex);
        }
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        if (!ctx.hasArgs() || ctx.args().getFirst().equalsIgnoreCase("random")) {
            sendRandomCatEmbed(ctx);
        } else if (ctx.args().getFirst().equalsIgnoreCase("breeds") || ctx.args().getFirst().equalsIgnoreCase("list")) {
            sendBreedListEmbed(ctx);
        } else {
            String input = ctx.argString().toLowerCase();
            TheCatApiClient.CatBreed breed = breeds.get(input);
            if (breed != null) {
                sendBreedCatEmbed(ctx, breed);
            } else if (breeds.isEmpty()) {
                ctx.reply().errorEmbed("Breed data could not be loaded. Try again with just `" + ctx.prefix().orElse("") + "cat` for a random cat!");
            } else {
                ctx.reply().errorEmbed("Unknown breed **" + ctx.argString() + "**. Use `" + ctx.prefix().orElse("") + "cat breeds` to see the full list.");
            }
        }
    }

    private void sendRandomCatEmbed(CommandContext ctx) {
        String url;
        try {
            url = TheCatApiClient.getRandomImage();
        } catch (Exception ex) {
            ctx.reply().errorEmbed("Failed to fetch a cat image. Try again later!");
            LOGGER.error("Error fetching random cat image.", ex);
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Here is your cat!");
        builder.setImage(url);
        builder.setColor(getEmbedColor());
        ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES, null, ignored -> {
        }));
    }

    private void sendBreedListEmbed(CommandContext ctx) {
        if (breeds.isEmpty()) {
            ctx.reply().errorEmbed("Breed data could not be loaded. Try again later!");
            return;
        }

        String list = Formatter.asCodeBlock(String.join(", ", breeds.values().stream()
            .map(TheCatApiClient.CatBreed::name)
            .toList()));

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Available cat breeds");
        builder.setDescription(list);
        builder.setColor(getEmbedColor());
        ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES, null, ignored -> {
        }));
    }

    private void sendBreedCatEmbed(CommandContext ctx, TheCatApiClient.CatBreed breed) {
        String url;
        try {
            url = TheCatApiClient.getRandomBreedImage(breed.id());
        } catch (Exception ex) {
            ctx.reply().errorEmbed("Failed to fetch a cat image for that breed. Try again later!");
            LOGGER.error("Error fetching cat image for breed '{}'.", breed.id(), ex);
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Here is your " + breed.name() + "!");
        builder.setImage(url);
        builder.setColor(getEmbedColor());
        ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES, null, ignored -> {
        }));
    }
}
