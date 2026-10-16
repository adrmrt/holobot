package dev.zawarudo.holo.modules.pokemon;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import dev.zawarudo.holo.database.Database;
import dev.zawarudo.holo.database.FlywayMigrator;
import dev.zawarudo.holo.database.SQLManager;
import dev.zawarudo.holo.database.dao.PokemonDao;
import dev.zawarudo.holo.database.dao.PokemonDao.SpawnRow;
import dev.zawarudo.holo.modules.pokemon.FakeDiscord.FakeMessage;
import dev.zawarudo.holo.modules.pokemon.PokemonSpawnManager.GuessResult;
import dev.zawarudo.holo.modules.pokemon.PokemonSpawnManager.WildPokemon;
import dev.zawarudo.holo.modules.pokemon.model.Pokemon;
import dev.zawarudo.holo.modules.pokemon.model.PokemonSpecies;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * A {@link PokemonSpawnManager} wired to a fresh database, a {@link FakeDiscord} and a few fixed
 * Pokemon instead of PokeAPI. Executor work runs immediately; the sweep runs when a test calls
 * {@link #sweep()}.
 */
final class SpawnFixture {

    static final long GUILD_ID = 100;
    static final long CHANNEL_ID = 200;

    static final TestPokemon CHARIZARD = TestPokemon.of(6, "Charizard", "Glurak", false, false);
    static final TestPokemon NIDORAN_F = TestPokemon.of(29, "Nidoran♀", "Nidoran♀", false, false);
    static final TestPokemon MEWTWO = TestPokemon.of(150, "Mewtwo", "Mewtu", true, false);
    static final TestPokemon MEW = TestPokemon.of(151, "Mew", "Mew", false, true);
    private static final List<TestPokemon> ALL = List.of(CHARIZARD, NIDORAN_F, MEWTWO, MEW);

    /** A species and its default Pokemon, as PokeAPI would return them. */
    record TestPokemon(int id, String en, PokemonSpecies species, Pokemon pokemon) {

        private static final Gson GSON = new Gson();

        static TestPokemon of(int id, String en, String de, boolean legendary, boolean mythical) {
            String species = """
                {"id": %d, "is_legendary": %b, "is_mythical": %b, "names": [
                  {"name": "%s", "language": {"name": "en"}},
                  {"name": "%s", "language": {"name": "de"}}]}
                """.formatted(id, legendary, mythical, en, de);
            String pokemon = """
                {"id": %d, "sprites": {"other": {"official-artwork": {
                  "front_default": "%s", "front_shiny": "%s"}}}}
                """.formatted(id, artwork(id, false), artwork(id, true));
            return new TestPokemon(id, en, GSON.fromJson(species, PokemonSpecies.class), GSON.fromJson(pokemon, Pokemon.class));
        }

        static String artwork(int id, boolean shiny) {
            return "https://example.com/" + id + (shiny ? "-shiny" : "") + ".png";
        }
    }

    final PokemonDao dao;
    final FakeDiscord discord = new FakeDiscord(GUILD_ID);
    final TextChannel channel = discord.addChannel(CHANNEL_ID);
    final PokemonSpawnManager manager;

    /** What the next random spawn will be. */
    TestPokemon next = CHARIZARD;
    boolean nextShiny;
    boolean enabled = true;

    private Runnable sweep;

    SpawnFixture(Path dir) throws Exception {
        String dbPath = dir.resolve("holo.db").toString();
        FlywayMigrator.migrate(dbPath);
        Database.setDbPath(dbPath);
        dao = new PokemonDao(new SQLManager());

        // Keeps the sweep instead of running it every minute
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1) {
            @Override
            public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
                sweep = command;
                return null;
            }
        };

        manager = new PokemonSpawnManager(dao, null, discord.jda, scheduler, MoreExecutors.newDirectExecutorService()) {
            @Override
            PokemonSpecies fetchRandomSpecies() {
                return next.species();
            }

            @Override
            Pokemon fetchPokemon(int id) {
                return byId(id).pokemon();
            }

            @Override
            PokemonSpecies speciesOf(Pokemon pokemon) {
                return byId(pokemon.getPokedexId()).species();
            }

            @Override
            Pokemon pokemonOf(PokemonSpecies species) {
                return byId(species.getPokedexId()).pokemon();
            }

            @Override
            BufferedImage drawSilhouette(Pokemon pokemon) {
                return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            }

            @Override
            boolean rollShiny() {
                return nextShiny;
            }

            @Override
            public boolean isEnabled(Guild guild) {
                return enabled;
            }
        };
    }

    /** Starts the manager like the bot does at startup: restores saved spawns and schedules the sweep. */
    void start() {
        manager.start();
    }

    /** Runs one sweep, as the scheduler does every minute. */
    void sweep() {
        sweep.run();
    }

    /** Saves a Pokemon as if it had spawned before a restart. Returns its spawn message id. */
    long savedBeforeRestart(TestPokemon pokemon, boolean shiny, long spawnedAt) throws SQLException {
        long messageId = discord.postEarlier(CHANNEL_ID).id;
        dao.insertSpawn(CHANNEL_ID, GUILD_ID);
        dao.updateSpawn(CHANNEL_ID, pokemon.id(), shiny, messageId, spawnedAt);
        return messageId;
    }

    WildPokemon wild() {
        return manager.getWild(CHANNEL_ID).orElseThrow();
    }

    GuessResult guess(long userId, String name) {
        return manager.guess(CHANNEL_ID, wild().messageId(), userId, name);
    }

    /** Guesses right and completes the catch, like the catch listener does. */
    void catchAs(long userId, String userName, String guess) {
        GuessResult.Caught caught = assertInstanceOf(GuessResult.Caught.class, guess(userId, guess));
        manager.completeCatch(caught.pokemon(), FakeDiscord.user(userId, userName), userName);
    }

    FakeMessage lastMessage() {
        return discord.messages(CHANNEL_ID).getLast();
    }

    SpawnRow savedSpawn() throws SQLException {
        return dao.findAllSpawns().stream().filter(r -> r.channelId() == CHANNEL_ID).findFirst().orElseThrow();
    }

    private static TestPokemon byId(int id) {
        return ALL.stream().filter(p -> p.id() == id).findFirst().orElseThrow();
    }
}
