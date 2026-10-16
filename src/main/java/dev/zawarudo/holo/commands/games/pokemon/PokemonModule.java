package dev.zawarudo.holo.commands.games.pokemon;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import dev.zawarudo.holo.commands.CommandManager;
import dev.zawarudo.holo.commands.CommandModule;
import dev.zawarudo.holo.database.dao.PokemonDao;
import dev.zawarudo.holo.modules.pokemon.PokemonSpawnManager;
import dev.zawarudo.holo.utils.UserResolver;

public class PokemonModule implements CommandModule {

    private final PokemonSpawnManager pokemonSpawnManager;
    private final PokemonDao pokemonDao;
    private final EventWaiter waiter;
    private final UserResolver userResolver;

    public PokemonModule(PokemonSpawnManager pokemonSpawnManager, PokemonDao pokemonDao, EventWaiter waiter, UserResolver userResolver) {
        this.pokemonSpawnManager = pokemonSpawnManager;
        this.pokemonDao = pokemonDao;
        this.waiter = waiter;
        this.userResolver = userResolver;
    }

    @Override
    public ModuleId id() {
        return ModuleId.POKEMON;
    }

    @Override
    public String description() {
        return "Commands related to Pokémon, such as catching Pokémon, managing teams, and viewing Pokédex entries.";
    }

    @Override
    public void register(CommandManager registry) {
        ModuleId moduleId = id();

        registry.addCommand(new PcCmd(pokemonDao, userResolver, waiter), moduleId);
        registry.addCommand(new PokedexCmd(pokemonDao), moduleId);
        registry.addCommand(new PokemonLeaderboardCmd(pokemonDao), moduleId);
        registry.addCommand(new PokemonTeamCmd(), moduleId);
        registry.addCommand(new SpawnCmd(pokemonSpawnManager), moduleId);
    }
}
