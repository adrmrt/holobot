package dev.zawarudo.holo.modules.pokemon;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.zawarudo.holo.modules.pokemon.model.EvolutionChain;
import dev.zawarudo.holo.modules.pokemon.model.Pokemon;
import dev.zawarudo.holo.modules.pokemon.model.PokemonSpecies;
import dev.zawarudo.holo.modules.pokemon.model.PokemonType;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.exceptions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

public final class PokeApiClient {

    /**
     * The base URL for the PokeAPI.
     */
    public static final String BASE_URL = "https://pokeapi.co/api/v2/";
    /**
     * The number of Pokémon that exist.
     */
    public static final int POKEMON_COUNT = 1025;

    private static final Gson GSON = new Gson();

    private PokeApiClient() {
    }

    /**
     * Returns a {@link PokemonSpecies} object from a given Pokédex id.
     *
     * @param id The Pokédex id of the desired Pokémon species.
     * @return A {@link PokemonSpecies} object
     */
    public static PokemonSpecies getPokemonSpecies(int id) throws InvalidIdException, APIException {
        String url = BASE_URL + "pokemon-species/" + id + "/";
        try {
            JsonObject obj = fetchJsonOrThrow(url);
            return GSON.fromJson(obj, PokemonSpecies.class);
        } catch (NotFoundException | InvalidRequestException _) {
            throw new InvalidIdException("Invalid Pokédex id: " + id);
        }
    }

    /**
     * Returns a {@link PokemonSpecies} object from a given Pokémon species name.
     *
     * @param name The name of the desired Pokémon species.
     * @return A {@link PokemonSpecies} object
     */
    public static PokemonSpecies getPokemonSpecies(String name) throws NotFoundException, APIException {
        String url = BASE_URL + "pokemon-species/" + escape(name) + "/";
        try {
            JsonObject obj = fetchJsonOrThrow(url);
            return GSON.fromJson(obj, PokemonSpecies.class);
        } catch (NotFoundException | InvalidRequestException _) {
            throw new NotFoundException("Invalid Pokémon species: " + name);
        }
    }

    /**
     * Returns a {@link Pokemon} object from a given Pokédex id.
     *
     * @param id The Pokédex id of the desired Pokémon.
     * @return A {@link Pokemon} object
     */
    public static Pokemon getPokemon(int id) throws InvalidIdException, APIException {
        String url = BASE_URL + "pokemon/" + id + "/";
        try {
            JsonObject obj = fetchJsonOrThrow(url);
            return GSON.fromJson(obj, Pokemon.class);
        } catch (NotFoundException | InvalidRequestException _) {
            throw new InvalidIdException("Invalid Pokédex id: " + id);
        }
    }

    /**
     * Returns a {@link Pokemon} object from a given Pokémon name.
     *
     * @param name The name of the desired Pokémon.
     * @return A {@link Pokemon} object
     */
    public static Pokemon getPokemon(String name) throws NotFoundException, APIException {
        String url = BASE_URL + "pokemon/" + escape(name) + "/";
        try {
            JsonObject obj = fetchJsonOrThrow(url);
            return GSON.fromJson(obj, Pokemon.class);
        } catch (NotFoundException | InvalidRequestException e) {
            throw new NotFoundException("Invalid Pokémon name: " + name, e);
        }
    }

    /**
     * Fetches all the information regarding the given type ID.
     */
    public static PokemonType getType(int typeId) throws IllegalArgumentException, APIException {
        String url = String.format("%s/type/%d/", BASE_URL, typeId);
        try {
            JsonObject obj = fetchJsonOrThrow(url);
            return GSON.fromJson(obj, PokemonType.class);
        } catch (NotFoundException | InvalidRequestException _) {
            throw new IllegalArgumentException("Invalid Pokémon type id: " + typeId);
        }
    }

    /**
     * Fetches all the information regarding the given type name.
     */
    public static PokemonType getType(String name) throws IllegalArgumentException, APIException {
        String url = String.format("%s/type/%s/", BASE_URL, escape(name));
        try {
            JsonObject obj = fetchJsonOrThrow(url);
            return GSON.fromJson(obj, PokemonType.class);
        } catch (NotFoundException | InvalidRequestException _) {
            throw new IllegalArgumentException("Invalid Pokémon type name: " + name);
        }
    }

    /**
     * Returns a random {@link PokemonSpecies} object from all existing Pokémon species.
     *
     * @return A {@link PokemonSpecies} object
     */
    public static PokemonSpecies getRandomPokemonSpecies() throws APIException {
        final int maxAttempts = 20;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            int id = nextRandomId();
            try {
                return getPokemonSpecies(id);
            } catch (InvalidIdException _) {
                // Gap/out-of-date count: try another id
            }
        }

        throw new APIException("Failed to fetch a random Pokémon species after " + maxAttempts + " attempts (possible ID gaps or outdated POKEMON_COUNT).");
    }

    /**
     * Returns a random {@link Pokemon} object from all existing Pokémon species.
     *
     * @return A {@link Pokemon} object
     */
    public static Pokemon getRandomPokemon() throws APIException {
        final int maxAttempts = 20;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            int id = nextRandomId();
            try {
                return getPokemon(id);
            } catch (InvalidIdException _) {
                // Gap/out-of-date count: try another id
            }
        }
        throw new APIException("Failed to fetch a random Pokémon after " + maxAttempts + " attempts (possible ID gaps or outdated POKEMON_COUNT).");
    }

    public static EvolutionChain getEvolutionChainByUrl(String url) throws APIException {
        try {
            JsonObject obj = HoloHttp.getJsonObject(url);
            return GSON.fromJson(obj, EvolutionChain.class);
        } catch (HttpStatusException e) {
            int code = e.getStatusCode();
            throw new APIException("PokéAPI returned HTTP " + code + " for evolution chain: " + url, e);
        } catch (HttpTransportException e) {
            throw new APIException("Transport error while requesting evolution chain: " + url, e);
        }
    }

    private static int nextRandomId() {
        try {
            return getRandomNumberFromRandomOrg();
        } catch (Exception _) {
            // fallback
            return 1 + java.util.concurrent.ThreadLocalRandom.current().nextInt(POKEMON_COUNT);
        }
    }

    private static int getRandomNumberFromRandomOrg() throws APIException {
        String url = "https://www.random.org/integers/?num=1&min=1&max=" + POKEMON_COUNT + "&col=1&base=10&format=plain";
        try {
            String line = HoloHttp.readLine(url).trim();
            return Integer.parseInt(line);
        } catch (HttpStatusException e) {
            throw new APIException("random.org returned HTTP " + e.getStatusCode(), e);
        } catch (HttpTransportException e) {
            throw new APIException("I/O error contacting random.org", e);
        } catch (NumberFormatException e) {
            throw new APIException("random.org returned an invalid number", e);
        }
    }

    /**
     * Fetches all the given ids and returns a list of {@link Pokemon}. Uses
     * parallelization to be as quick as possible.
     *
     * @param ids An array of Pokémon ids.
     */
    public static List<Pokemon> getPokemon(int... ids) throws InterruptedException, ExecutionException {
        int poolSize = Math.min(ids.length, Runtime.getRuntime().availableProcessors());

        try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
            List<Future<Pokemon>> futures = new ArrayList<>();

            for (int id : ids) {
                Callable<Pokemon> task = () -> {
                    try {
                        return PokeApiClient.getPokemon(id);
                    } catch (InvalidIdException _) {
                        return null;
                    }
                };
                futures.add(executor.submit(task));
            }

            List<Pokemon> pokemon = new ArrayList<>();
            for (Future<Pokemon> future : futures) {
                pokemon.add(future.get());
            }

            return pokemon;
        }
    }

    /**
     * Replaces weird characters.
     */
    private static String escape(String name) {
        return name.toLowerCase(Locale.UK).replace(" ", "-")
            .replace(".", "")
            .replace(":", "-")
            .replace("'", "")
            .replace("♀", "-f")
            .replace("♂", "-m")
            .replace(":female_sign:", "-f")
            .replace(":male_sign:", "-m");
    }

    private static JsonObject fetchJsonOrThrow(String url) throws NotFoundException, APIException, InvalidRequestException {
        try {
            return HoloHttp.getJsonObject(url);
        } catch (HttpStatusException e) {
            int code = e.getStatusCode();
            if (code == 404) {
                throw new NotFoundException("Not found: " + url, e);
            }
            if (code >= 400 && code < 500) {
                throw new InvalidRequestException("Invalid request (" + code + "): " + url, e);
            }
            throw new APIException("API error (" + code + "): " + url, e);
        } catch (HttpTransportException e) {
            throw new APIException("I/O error while contacting PokéAPI: " + url, e);
        }
    }
}
