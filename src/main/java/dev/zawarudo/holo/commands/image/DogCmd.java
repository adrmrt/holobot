package dev.zawarudo.holo.commands.image;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.DogCeoClient;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.Reader;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "dog",
    description = "Fetches an image of a dog.",
    usage = "[breeds | <breed> | random]",
    category = CommandCategory.IMAGE)
public class DogCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogCmd.class);

    private static final String RESOURCE_PATH = "data/dog-breeds.json";
    private final String[] breeds;
    private final Map<String, String> formattedNames;

    public DogCmd() {
        formattedNames = new HashMap<>();

        try {
            JsonObject obj = Reader.readJsonObjectResource(RESOURCE_PATH);
            breeds = new Gson().fromJson(obj.getAsJsonArray("breeds"), String[].class);

            obj.getAsJsonArray("breeds-formatted").forEach(breed -> {
                String name = breed.getAsJsonObject().get("name").getAsString();
                String formatted = breed.getAsJsonObject().get("formattedName").getAsString();
                formattedNames.put(name, formatted);
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Something went wrong while reading the dog breeds file!", ex);
        }
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        if (!ctx.hasArgs() || ctx.args().getFirst().equalsIgnoreCase("random")) {
            sendRandomDogEmbed(ctx);
        } else if (ctx.args().getFirst().equalsIgnoreCase("breeds") || ctx.args().getFirst().equalsIgnoreCase("list")) {
            sendBreedListEmbed(ctx);
        } else if (Arrays.stream(breeds).anyMatch(b -> b.equalsIgnoreCase(ctx.args().getFirst()))) {
            sendDogImageEmbed(ctx, ctx.args().getFirst());
        } else {
            ctx.reply().errorEmbed("The breed you specified is unknown. Use `" + ctx.prefix().orElse("") + "dog breeds` to see a list of available breeds.");
        }
    }

    private void sendRandomDogEmbed(CommandContext ctx) {
        String url;
        try {
            url = DogCeoClient.getRandomImage();
        } catch (APIException ex) {
            ctx.reply().errorEmbed("An error occurred while fetching the image from the API. Try again later!");
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("An error occurred while fetching the image from the API.", ex);
            }
            return;
        }
        String breed = url.split("/")[4];

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Here is your random dog!");
        builder.setDescription("It's a **" + getFormattedName(breed) + "**!");
        builder.setImage(url);
        builder.setColor(getEmbedColor());
        ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES, null, ignored -> {
        }));
    }

    private void sendBreedListEmbed(CommandContext ctx) {
        String s = Formatter.asCodeBlock(String.join(", ", breeds));
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Available dog breeds");
        builder.setDescription(s);
        builder.setColor(getEmbedColor());
        ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES, null, ignored -> {
        }));
    }

    private void sendDogImageEmbed(CommandContext ctx, String breed) {
        String url;
        try {
            url = DogCeoClient.getRandomBreedImage(breed);
        } catch (APIException | InvalidRequestException ex) {
            ctx.reply().errorEmbed("An error occurred while fetching the image from the API. Try again later!");
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("An error occurred while fetching the image from the API.", ex);
            }
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Here is your " + getFormattedName(breed) + "!");
        builder.setImage(url);
        builder.setColor(getEmbedColor());
        ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES, null, ignored -> {
        }));
    }

    /**
     * Gets the formatted name of a breed.
     *
     * @param name The standard name as given by the API.
     * @return The formatted name to be displayed in the embed.
     */
    private String getFormattedName(String name) {
        return formattedNames.getOrDefault(name, Formatter.capitalize(name));
    }
}
