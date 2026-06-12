package dev.zawarudo.holo.commands.games.pokemon;

import dev.zawarudo.holo.modules.pokemon.PokeApiClient;
import dev.zawarudo.holo.modules.pokemon.model.Pokemon;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.exceptions.*;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandInfo(name = "spawn",
    description = "Spawns a Pokémon",
    usage = "[<Pokémon name or id> | random]",
    ownerOnly = true,
    category = CommandCategory.GAMES)
public class SpawnCmd implements CommandMetadata, ExecutableCommand {

    private final PokemonSpawnManager pokemonSpawnManager;

    public SpawnCmd(PokemonSpawnManager pokemonSpawnManager) {
        this.pokemonSpawnManager = pokemonSpawnManager;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        long channelId = ctx.channel().getIdLong();
        List<String> args = ctx.args();

        try {
            if (args.isEmpty() || args.size() == 1 && "random".equalsIgnoreCase(args.getFirst())) {
                // Random spawn
                Pokemon pokemon = PokeApiClient.getRandomPokemonSpecies().getPokemon();
                pokemonSpawnManager.deleteMessage(channelId);
                pokemonSpawnManager.spawnNewPokemon(channelId, pokemon);
                return;
            } else if ("add".equalsIgnoreCase(args.getFirst())) {
                // Make Pokémon spawn in a new text channel
                pokemonSpawnManager.addChannel(channelId);

                Pokemon pokemon = PokeApiClient.getRandomPokemonSpecies().getPokemon();
                pokemonSpawnManager.spawnNewPokemon(channelId, pokemon);
                return;
            }
            // Spawn specific Pokémon
            String first = args.getFirst();
            Pokemon pokemon = isNumeric(first)
                ? PokeApiClient.getPokemon(Integer.parseInt(first))
                : PokeApiClient.getPokemon(first);

            pokemonSpawnManager.deleteMessage(channelId);
            pokemonSpawnManager.spawnNewPokemon(channelId, pokemon);
        } catch (APIException _) {
            sendOwnerError(ctx, "PokéAPI error right now. Try again later.");
        } catch (NotFoundException | InvalidIdException _) {
            sendOwnerError(ctx, "Pokémon not found. Check typos / ID.");
        }
    }

    private static boolean isNumeric(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return !s.isBlank();
    }

    private void sendOwnerError(CommandContext ctx, String msg) {
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle("Spawn error");
        b.setDescription(msg);
        ctx.notifyOwner(b);
    }
}
