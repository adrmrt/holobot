package dev.zawarudo.holo.modules.pokemon;

import dev.zawarudo.holo.modules.pokemon.model.Pokemon;
import dev.zawarudo.holo.modules.pokemon.model.PokemonSpecies;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PokeApiClientIT {

    @Test
    @Timeout(10)
    void getPokemon_byId_mapsFields() throws Exception {
        Pokemon pikachu = PokeApiClient.getPokemon(25);

        assertEquals("Pikachu", pikachu.getName());
        assertEquals(25, pikachu.getPokedexId());
        assertEquals(List.of("Electric"), pikachu.getTypes());
        assertNotNull(pikachu.getSprites().getFrontDefault());
    }

    @Test
    @Timeout(10)
    void getPokemonSpecies_byName_mapsFields() throws Exception {
        PokemonSpecies species = PokeApiClient.getPokemonSpecies("charmander");

        assertEquals(4, species.getPokedexId());
        assertNotNull(species.getEvolutionChainUrl());
    }

    @Test
    @Timeout(15)
    void getEvolutionChain_resolves() throws Exception {
        String chain = PokeApiClient.getPokemonSpecies("charmander").getEvolutionChainString();

        assertNotNull(chain, "No evolution chain formatted for Charmander");
        assertTrue(chain.toLowerCase().contains("charizard"), "Unexpected chain: " + chain);
    }

    @Test
    @Timeout(10)
    void getType_byName_works() throws Exception {
        assertEquals("fire", PokeApiClient.getType("fire").getName());
    }

    @Test
    @Timeout(10)
    void getPokemon_unknownName_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> PokeApiClient.getPokemon("notapokemon"));
    }

    // Fails when a new game adds species, so random spawns/teams can include them.
    // Uses pokemon-species because /pokemon also counts alternate forms.
    @Test
    @Timeout(10)
    void pokemonCount_matchesSpeciesCount() throws Exception {
        int count = HoloHttp.getJsonObject(PokeApiClient.BASE_URL + "pokemon-species?limit=1").get("count").getAsInt();

        assertEquals(PokeApiClient.POKEMON_COUNT, count, "PokeAPI lists " + count + " species - update POKEMON_COUNT");
    }
}
