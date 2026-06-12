package dev.zawarudo.holo.commands.games.pokemon;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.pokemon.PokeApiClient;
import dev.zawarudo.holo.modules.pokemon.model.Pokemon;
import dev.zawarudo.holo.utils.ImageOperations;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "pokemonteam",
    description = "Generates a Pokémon team",
    usage = "random",
    alias = {"poketeam"},
    guildOnly = false,
    category = CommandCategory.GAMES)
public class PokemonTeamCmd implements CommandMetadata, ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        EmbedBuilder builder = new EmbedBuilder();

        // Display help page
        if (!ctx.hasArgs()) {
            ctx.channel().sendMessage("This feature is in development and thus not available yet. " +
                "You probably meant `" + ctx.prefix().orElse("") + "pokemonteam random`").queue();
        } else if ("random".equals(ctx.args().getFirst())) {
            ctx.reply().typing();

            InputStream input;
            try {
                // Generate 6 random Pokémon ids
                List<Integer> ids = new ArrayList<>();
                for (int i = 0; i < 6; i++) {
                    ids.add(new Random().nextInt(PokeApiClient.POKEMON_COUNT) + 1);
                }
                List<Pokemon> pokemon = PokeApiClient.getPokemon(ids.stream().mapToInt(k -> k).toArray());
                PokemonTeam team = new PokemonTeam(pokemon.toArray(new Pokemon[0]));
                BufferedImage img = team.generateTeamImage();
                input = ImageOperations.toInputStream(img);
            } catch (IOException | InterruptedException | ExecutionException ex) {
                builder.setTitle("Error");
                builder.setDescription("Something went wrong while creating a Pokémon team. Please try again in a few minutes!");
                ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(15, TimeUnit.SECONDS, null, ignored -> {
                }));
                return;
            }

            builder.setTitle("Random Pokémon Team");
            builder.setImage("attachment://pokemonteam.png");
            ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl()));
            FileUpload upload = FileUpload.fromData(input, "pokemonteam.png");
            ctx.channel().sendFiles(upload).setEmbeds(builder.build()).queue();
        } else {
            // Add more stuff in the future, like the ability to create a custom team for users
            ctx.channel().sendMessage("This feature is in development and thus not available yet. You probably meant `" + ctx.prefix().orElse("") + "pokemonteam random`").queue();
        }
    }
}
