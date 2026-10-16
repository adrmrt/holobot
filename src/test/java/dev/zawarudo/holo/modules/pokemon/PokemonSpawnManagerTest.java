package dev.zawarudo.holo.modules.pokemon;

import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.database.dao.PokemonDao.CollectionEntry;
import dev.zawarudo.holo.modules.pokemon.FakeDiscord.FakeMessage;
import dev.zawarudo.holo.modules.pokemon.PokemonSpawnManager.GuessResult.Caught;
import dev.zawarudo.holo.modules.pokemon.PokemonSpawnManager.GuessResult.Gone;
import dev.zawarudo.holo.modules.pokemon.PokemonSpawnManager.GuessResult.NoGuessesLeft;
import dev.zawarudo.holo.modules.pokemon.PokemonSpawnManager.GuessResult.Wrong;
import dev.zawarudo.holo.modules.pokemon.PokemonSpawnManager.WildPokemon;
import dev.zawarudo.holo.modules.pokemon.SpawnFixture.TestPokemon;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static dev.zawarudo.holo.modules.pokemon.SpawnFixture.CHANNEL_ID;
import static dev.zawarudo.holo.modules.pokemon.SpawnFixture.CHARIZARD;
import static dev.zawarudo.holo.modules.pokemon.SpawnFixture.GUILD_ID;
import static dev.zawarudo.holo.modules.pokemon.SpawnFixture.MEW;
import static dev.zawarudo.holo.modules.pokemon.SpawnFixture.MEWTWO;
import static dev.zawarudo.holo.modules.pokemon.SpawnFixture.NIDORAN_F;
import static org.junit.jupiter.api.Assertions.*;

class PokemonSpawnManagerTest {

    private static final long ASH = 1;
    private static final long MISTY = 2;
    private static final Color SHINY_GOLD = new Color(255, 215, 0);

    @TempDir
    Path dir;

    private SpawnFixture f;

    @BeforeEach
    void setUp() throws Exception {
        f = new SpawnFixture(dir);
    }

    @Test
    void ratesMatchTheDesign() {
        // The manual test plan lowers these temporarily; this fails if that gets committed
        assertEquals(512, PokemonSpawnManager.SHINY_ONE_IN);
        assertEquals(10, PokemonSpawnManager.LEGENDARY_KEEP_ONE_IN);
        assertEquals(3, PokemonSpawnManager.MAX_WRONG_GUESSES);
    }

    @Test
    void addingChannelSpawnsPokemonAndSavesIt() throws Exception {
        f.start();

        assertTrue(f.manager.addChannel(f.channel));

        FakeMessage spawn = f.lastMessage();
        assertEquals("A wild Pokémon appeared!", spawn.embed.getTitle());
        // Buttons on spawn messages from older versions must keep working
        assertEquals(List.of("pokemon:catch"), spawn.buttonIds);
        assertEquals(spawn.id, f.wild().messageId());
        assertEquals(CHARIZARD.id(), f.savedSpawn().pokemonId());
        assertEquals(spawn.id, f.savedSpawn().messageId());
        assertEquals(List.of(CHANNEL_ID), f.manager.getChannels(GUILD_ID));

        assertFalse(f.manager.addChannel(f.channel), "already a spawn channel");
        assertEquals(1, f.discord.messages.size());
    }

    @Test
    void catchSavesPointsRevealsAndSpawnsNext() throws Exception {
        f.start();
        f.manager.addChannel(f.channel);
        long first = f.wild().messageId();

        f.catchAs(ASH, "Ash", "Charizard");

        FakeMessage reveal = f.discord.message(first);
        assertEquals("The wild Charizard has been caught!", reveal.embed.getTitle());
        assertEquals("Caught by Ash • +1 point (server total: 1)", reveal.embed.getFooter().getText());
        assertEquals(CHARIZARD.pokemon().getSprites().getOther().getArtwork().getFrontDefault(), reveal.embed.getImage().getUrl());
        assertTrue(reveal.buttonIds.isEmpty(), "Catch button removed");
        assertEquals(Duration.ofMinutes(2).toSeconds(), reveal.deleteAfterSeconds);
        assertEquals(List.of(new CollectionEntry(CHARIZARD.id(), "Charizard", 1, 0)), f.dao.findCollection(ASH));

        long second = f.wild().messageId();
        assertNotEquals(first, second, "next Pokemon spawned");
        assertEquals(second, f.savedSpawn().messageId());

        f.catchAs(ASH, "Ash", "Charizard");
        assertEquals("Caught by Ash • +1 point (server total: 2)", f.discord.message(second).embed.getFooter().getText());
    }

    static Stream<Arguments> rarities() {
        return Stream.of(
            Arguments.of(CHARIZARD, true, 5, "✨ Shiny Charizard caught! ✨", "This was a shiny Pokémon!"),
            Arguments.of(MEWTWO, false, 10, "The wild Mewtwo has been caught!", "This was a legendary Pokémon!"),
            Arguments.of(MEW, false, 10, "The wild Mew has been caught!", "This was a mythical Pokémon!"),
            Arguments.of(MEWTWO, true, 50, "✨ Shiny Mewtwo caught! ✨", "This was a legendary Pokémon!\nThis was a shiny Pokémon!")
        );
    }

    @ParameterizedTest
    @MethodSource("rarities")
    void rarePokemonGiveMorePoints(TestPokemon pokemon, boolean shiny, int points, String title, String description) throws Exception {
        f.next = pokemon;
        f.nextShiny = shiny;
        f.start();
        f.manager.addChannel(f.channel);
        long spawn = f.wild().messageId();

        f.catchAs(ASH, "Ash", pokemon.en());

        MessageEmbed reveal = f.discord.message(spawn).embed;
        assertEquals(title, reveal.getTitle());
        assertEquals(description, reveal.getDescription());
        assertEquals("Caught by Ash • +" + points + " points (server total: " + points + ")", reveal.getFooter().getText());
        assertEquals(TestPokemon.artwork(pokemon.id(), shiny), reveal.getImage().getUrl());
        assertEquals(shiny ? SHINY_GOLD : EmbedColor.POKEMON.getColor(), reveal.getColor());
        assertEquals(points, f.dao.findPoints(GUILD_ID, ASH));
        assertEquals(shiny ? 1 : 0, f.dao.findCollection(ASH).getFirst().shinies());
    }

    @Test
    void englishAndGermanNamesCatch() throws Exception {
        f.start();
        f.manager.addChannel(f.channel);

        f.catchAs(ASH, "Ash", "charizard");
        f.catchAs(ASH, "Ash", "Glurak");

        f.next = NIDORAN_F;
        f.manager.forceSpawn(CHANNEL_ID, null);
        f.catchAs(ASH, "Ash", "Nidoran F");
        assertEquals(new Wrong(2), f.guess(ASH, "Nidoran"), "plain Nidoran is ambiguous");
    }

    @Test
    void wrongGuessesAreLimitedPerPlayer() throws Exception {
        f.start();
        f.manager.addChannel(f.channel);

        assertEquals(new Wrong(2), f.guess(ASH, "Pikachu"));
        assertEquals(new Wrong(1), f.guess(ASH, "Pikachu"));
        assertEquals(new Wrong(0), f.guess(ASH, "Pikachu"));
        assertEquals(new NoGuessesLeft(), f.guess(ASH, "Charizard"));
        assertEquals(0, f.manager.guessesLeft(f.wild(), ASH));

        assertEquals(3, f.manager.guessesLeft(f.wild(), MISTY), "other players are unaffected");
        f.catchAs(MISTY, "Misty", "Charizard");

        assertEquals(3, f.manager.guessesLeft(f.wild(), ASH), "fresh guesses for the next Pokemon");
    }

    @Test
    void onlyTheFirstCorrectGuessCatches() throws Exception {
        f.start();
        f.manager.addChannel(f.channel);
        long spawn = f.wild().messageId();

        assertInstanceOf(Caught.class, f.manager.guess(CHANNEL_ID, spawn, ASH, "Charizard"));
        assertEquals(new Gone(), f.manager.guess(CHANNEL_ID, spawn, MISTY, "Charizard"));
    }

    @Test
    void guessForOldSpawnMessageIsGone() throws Exception {
        f.start();
        f.manager.addChannel(f.channel);
        long old = f.wild().messageId();
        f.catchAs(ASH, "Ash", "Charizard");

        assertEquals(new Gone(), f.manager.guess(CHANNEL_ID, old, MISTY, "Charizard"));
        assertEquals(3, f.manager.guessesLeft(f.wild(), MISTY), "doesn't cost a guess on the new Pokemon");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pokemonFleesAfterSixHours(boolean shiny) throws Exception {
        long spawn = f.savedBeforeRestart(CHARIZARD, shiny, minutesAgo(6 * 60 + 1));
        f.start();

        f.sweep();

        FakeMessage reveal = f.discord.message(spawn);
        assertEquals(shiny ? "✨ The shiny Charizard fled... ✨" : "The wild Charizard fled!", reveal.embed.getTitle());
        assertNull(reveal.embed.getDescription(), "rarity is only revealed on a catch");
        assertTrue(reveal.buttonIds.isEmpty());
        assertEquals(Duration.ofMinutes(2).toSeconds(), reveal.deleteAfterSeconds);
        assertTrue(f.dao.findCollection(ASH).isEmpty());
        assertNotEquals(spawn, f.wild().messageId(), "next Pokemon spawned");
    }

    @Test
    void pokemonStaysUntilSixHours() throws Exception {
        long spawn = f.savedBeforeRestart(CHARIZARD, false, minutesAgo(6 * 60 - 1));
        f.start();

        f.sweep();

        assertFalse(f.discord.message(spawn).edited);
        assertEquals(spawn, f.wild().messageId());
        assertEquals(1, f.discord.messages.size());
    }

    @Test
    void restartKeepsWaitingPokemonCatchable() throws Exception {
        long spawnedAt = minutesAgo(60);
        long spawn = f.savedBeforeRestart(MEWTWO, true, spawnedAt);

        f.start();
        f.sweep();

        WildPokemon wild = f.wild();
        assertEquals(spawn, wild.messageId());
        assertEquals("Mewtwo", wild.name());
        assertTrue(wild.shiny());
        assertEquals(spawnedAt, wild.spawnedAt(), "flee timer continues");
        assertEquals(1, f.discord.messages.size(), "no extra spawn");

        f.catchAs(ASH, "Ash", "Mewtu");
        assertEquals(50, f.dao.findPoints(GUILD_ID, ASH));
    }

    @Test
    void spawnMessageDeletedWhileOfflineIsReplaced() throws Exception {
        long spawn = f.savedBeforeRestart(CHARIZARD, false, minutesAgo(10));
        f.discord.message(spawn).deleted = true;

        f.start();
        assertTrue(f.manager.getWild(CHANNEL_ID).isEmpty());
        assertTrue(f.manager.isSpawnChannel(CHANNEL_ID));

        f.sweep();
        assertNotEquals(spawn, f.wild().messageId());
    }

    @Test
    void emptySpawnChannelGetsPokemonOnSweep() throws Exception {
        // E.g. the bot stopped between a catch and the next spawn
        f.dao.insertSpawn(CHANNEL_ID, GUILD_ID);
        f.start();

        f.sweep();

        assertEquals(f.lastMessage().id, f.wild().messageId());
    }

    @Test
    void disabledModulePausesSpawningAndFleeing() throws Exception {
        long spawn = f.savedBeforeRestart(CHARIZARD, false, minutesAgo(7 * 60));
        long emptyChannel = 201;
        f.discord.addChannel(emptyChannel);
        f.dao.insertSpawn(emptyChannel, GUILD_ID);
        f.enabled = false;
        f.start();

        f.sweep();

        assertFalse(f.discord.message(spawn).edited, "didn't flee");
        assertTrue(f.discord.messages(emptyChannel).isEmpty(), "didn't spawn");

        f.enabled = true;
        f.sweep();

        assertTrue(f.discord.message(spawn).edited, "fled");
        assertEquals(1, f.discord.messages(emptyChannel).size(), "spawned");
    }

    @Test
    void removingChannelDeletesPokemonAndStopsSpawns() throws Exception {
        f.start();
        f.manager.addChannel(f.channel);
        long spawn = f.wild().messageId();

        assertTrue(f.manager.removeChannel(CHANNEL_ID));

        assertTrue(f.discord.message(spawn).deleted);
        assertTrue(f.dao.findAllSpawns().isEmpty());
        assertEquals(new Gone(), f.manager.guess(CHANNEL_ID, spawn, ASH, "Charizard"));
        f.sweep();
        assertEquals(1, f.discord.messages.size(), "no new spawn");
        assertFalse(f.manager.removeChannel(CHANNEL_ID), "already removed");
    }

    @Test
    void forcedSpawnReplacesWaitingPokemon() throws Exception {
        f.start();
        f.manager.addChannel(f.channel);
        long old = f.wild().messageId();

        f.manager.forceSpawn(CHANNEL_ID, MEWTWO.pokemon());

        assertTrue(f.discord.message(old).deleted);
        assertEquals("Mewtwo", f.wild().name());
        assertEquals(MEWTWO.id(), f.savedSpawn().pokemonId());
    }

    private static long minutesAgo(long minutes) {
        return System.currentTimeMillis() - Duration.ofMinutes(minutes).toMillis();
    }
}
