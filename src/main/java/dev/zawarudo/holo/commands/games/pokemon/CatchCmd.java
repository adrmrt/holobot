package dev.zawarudo.holo.commands.games.pokemon;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.modules.pokemon.model.Pokemon;
import dev.zawarudo.holo.modules.pokemon.model.PokemonSpecies;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.NotFoundException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Command to catch the current Pokémon in a guild channel.
 */
@CommandInfo(name = "catch",
    description = "Use this command to catch the current Pokémon of a text channel. " +
        "Note that you can type either the English or German name of the Pokémon.",
    usage = "<Pokémon name>",
    embedColor = EmbedColor.POKEMON,
    category = CommandCategory.GAMES)
public class CatchCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatchCmd.class);

    private final PokemonSpawnManager pokemonSpawnManager;

    public CatchCmd(PokemonSpawnManager pokemonSpawnManager) {
        this.pokemonSpawnManager = pokemonSpawnManager;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        Pokemon pokemon = pokemonSpawnManager.getPokemon(ctx.channel().getIdLong());

        // There are no Pokémon in this channel
        if (pokemon == null) {
            ctx.reply().errorEmbed("There are no Pokémon to catch in this channel!");
            return;
        }

        PokemonSpecies species;
        try {
            species = pokemon.getPokemonSpecies();
        } catch (APIException ex) {
            ctx.reply().errorEmbed("API error. Please try again later");
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("There has been an API error.", ex);
            }
            return;
        } catch (NotFoundException ex) {
            ctx.reply().errorEmbed("There has been an internal error that wasn't supposed to " +
                "happen. Please submit a bug report with the name of this Pokémon and the id of " +
                "the channel where this happened.");
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("There has been an internal error. Check for possible bug report.", ex);
            }
            return;
        }

        String guessed = String.join(" ", ctx.args()).toLowerCase(Locale.UK);

        // Wrong name
        if (!guessed.equals(species.getName("en").toLowerCase(Locale.UK)) &&
            !guessed.equals(species.getName("de").toLowerCase(Locale.UK))) {
            return;
        }

        String catcher = ctx.member().map(Member::getEffectiveName).orElseGet(() -> ctx.user().getName());

        Message msg = pokemonSpawnManager.getMessage(ctx.channel().getIdLong());
        msg.editMessageAttachments(new ArrayList<>()).queue();

        EmbedBuilder builder = new EmbedBuilder(msg.getEmbeds().getFirst());
        builder.clear();

        builder.setTitle("The wild " + species.getName("en") + " has been caught!");
        builder.setImage(pokemon.getSprites().getOther().getArtwork().getFrontDefault());
        builder.setColor(Color.RED);
        builder.setFooter(String.format("Caught by %s", catcher), ctx.user().getEffectiveAvatarUrl());

        msg.editMessageEmbeds(builder.build()).queue(m -> m.delete().queueAfter(2, TimeUnit.MINUTES));
        pokemonSpawnManager.spawnNewPokemon(ctx.channel().getIdLong());
    }
}
