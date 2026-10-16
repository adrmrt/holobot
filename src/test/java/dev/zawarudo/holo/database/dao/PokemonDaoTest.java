package dev.zawarudo.holo.database.dao;

import dev.zawarudo.holo.database.Database;
import dev.zawarudo.holo.database.FlywayMigrator;
import dev.zawarudo.holo.database.SQLManager;
import dev.zawarudo.holo.database.dao.PokemonDao.CollectionEntry;
import dev.zawarudo.holo.database.dao.PokemonDao.LeaderboardEntry;
import dev.zawarudo.holo.database.dao.PokemonDao.SpawnRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the Pokemon queries against a database migrated from scratch. */
class PokemonDaoTest {

    private static final long ASH = 1;
    private static final long MISTY = 2;
    private static final long BROCK = 3;
    private static final long GUILD = 100;
    private static final long OTHER_GUILD = 101;
    private static final long CHANNEL = 200;

    @TempDir
    Path dir;

    private PokemonDao dao;

    @BeforeEach
    void setUp() throws Exception {
        String dbPath = dir.resolve("holo.db").toString();
        FlywayMigrator.migrate(dbPath);
        Database.setDbPath(dbPath);
        dao = new PokemonDao(new SQLManager());
    }

    @Test
    void leaderboardSumsPointsPerServer() throws Exception {
        save(ASH, GUILD, 6, "Charizard", false, 1);
        save(ASH, GUILD, 150, "Mewtwo", false, 10);
        save(BROCK, GUILD, 6, "Charizard", true, 5);
        save(MISTY, GUILD, 6, "Charizard", true, 5);
        save(MISTY, OTHER_GUILD, 150, "Mewtwo", true, 50);

        assertEquals(List.of(
            new LeaderboardEntry(ASH, 11),
            new LeaderboardEntry(MISTY, 5),
            new LeaderboardEntry(BROCK, 5)
        ), dao.findLeaderboard(GUILD), "ties ordered by user id");
        assertEquals(11, dao.findPoints(GUILD, ASH));
        assertEquals(50, dao.findPoints(OTHER_GUILD, MISTY));
        assertEquals(0, dao.findPoints(OTHER_GUILD, ASH));
    }

    @Test
    void collectionCombinesServersPerSpecies() throws Exception {
        save(ASH, OTHER_GUILD, 150, "Mewtwo", true, 50);
        save(ASH, GUILD, 6, "Charizard", false, 1);
        save(ASH, OTHER_GUILD, 6, "Charizard", true, 5);
        save(MISTY, GUILD, 25, "Pikachu", false, 1);

        assertEquals(List.of(
            new CollectionEntry(6, "Charizard", 2, 1),
            new CollectionEntry(150, "Mewtwo", 1, 1)
        ), dao.findCollection(ASH));
        assertEquals(2, dao.countCatches(ASH, 6));
        assertEquals(0, dao.countCatches(ASH, 25));
    }

    @Test
    void resetDeletesOnlyThatServerOrPlayer() throws Exception {
        save(ASH, GUILD, 6, "Charizard", false, 1);
        save(ASH, GUILD, 25, "Pikachu", false, 1);
        save(MISTY, GUILD, 6, "Charizard", false, 1);
        save(ASH, OTHER_GUILD, 6, "Charizard", false, 1);

        assertEquals(2, dao.deleteCatches(GUILD, ASH));
        assertEquals(List.of(new LeaderboardEntry(MISTY, 1)), dao.findLeaderboard(GUILD));

        assertEquals(1, dao.deleteCatches(GUILD));
        assertTrue(dao.findLeaderboard(GUILD).isEmpty());

        assertEquals(1, dao.findPoints(OTHER_GUILD, ASH), "other servers keep their points");
        assertEquals(List.of(new CollectionEntry(6, "Charizard", 1, 0)), dao.findCollection(ASH));
    }

    @Test
    void spawnRowKeepsWaitingPokemonUntilCleared() throws Exception {
        SpawnRow empty = new SpawnRow(CHANNEL, GUILD, 0, false, 0, 0);
        SpawnRow waiting = new SpawnRow(CHANNEL, GUILD, 6, true, 999, 123);

        dao.insertSpawn(CHANNEL, GUILD);
        assertEquals(List.of(empty), dao.findAllSpawns());
        assertFalse(empty.hasPokemon());

        dao.updateSpawn(CHANNEL, 6, true, 999, 123);
        dao.insertSpawn(CHANNEL, GUILD);
        assertEquals(List.of(waiting), dao.findAllSpawns(), "adding the channel again keeps its Pokemon");
        assertTrue(waiting.hasPokemon());

        dao.clearSpawn(CHANNEL);
        assertEquals(List.of(empty), dao.findAllSpawns());

        dao.deleteSpawn(CHANNEL);
        assertTrue(dao.findAllSpawns().isEmpty());
    }

    private void save(long userId, long guildId, int speciesId, String name, boolean shiny, int points) throws SQLException {
        dao.insertCatch(userId, guildId, speciesId, name, shiny, points, System.currentTimeMillis());
    }
}
