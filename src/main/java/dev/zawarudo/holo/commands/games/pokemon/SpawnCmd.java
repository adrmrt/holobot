package dev.zawarudo.holo.commands.games.pokemon;

import dev.zawarudo.holo.modules.pokemon.PokeApiClient;
import dev.zawarudo.holo.modules.pokemon.model.Pokemon;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.utils.exceptions.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

@CommandInfo(name = "spawn",
    description = "Spawns a Pokémon",
    usage = "[<Pokémon name or id> | random]",
    ownerOnly = true,
    category = CommandCategory.GAMES)
public class SpawnCmd extends AbstractCommand {

    private final PokemonSpawnManager pokemonSpawnManager;

    public SpawnCmd(PokemonSpawnManager pokemonSpawnManager) {
        this.pokemonSpawnManager = pokemonSpawnManager;
    }

    @Override
    public void onCommand(@NotNull MessageReceivedEvent event) {
        deleteInvoke(event);

        long channelId = event.getChannel().getIdLong();

        try {
            if (args.length == 0 || args.length == 1 && "random".equalsIgnoreCase(args[0])) {
                // Random spawn
                Pokemon pokemon = PokeApiClient.getRandomPokemonSpecies().getPokemon();
                pokemonSpawnManager.deleteMessage(channelId);
                pokemonSpawnManager.spawnNewPokemon(channelId, pokemon);
                return;
            } else if ("add".equalsIgnoreCase(args[0])) {
                // Make Pokémon spawn in a new text channel
                pokemonSpawnManager.addChannel(channelId);

                Pokemon pokemon = PokeApiClient.getRandomPokemonSpecies().getPokemon();
                pokemonSpawnManager.spawnNewPokemon(channelId, pokemon);
                return;
            }
            // Spawn specific Pokémon
            Pokemon pokemon = isNumeric(args[0])
                ? PokeApiClient.getPokemon(Integer.parseInt(args[0]))
                : PokeApiClient.getPokemon(args[0]);

            pokemonSpawnManager.deleteMessage(channelId);
            pokemonSpawnManager.spawnNewPokemon(channelId, pokemon);
        } catch (APIException ex) {
            sendOwnerError("PokéAPI error right now. Try again later.");
        } catch (NotFoundException | InvalidIdException ex) {
            sendOwnerError("Pokémon not found. Check typos / ID.");
        }
    }

    private static boolean isNumeric(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return !s.isBlank();
    }

    private void sendOwnerError(String msg) {
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle("Spawn error");
        b.setDescription(msg);
        sendToOwner(b);
    }
}
