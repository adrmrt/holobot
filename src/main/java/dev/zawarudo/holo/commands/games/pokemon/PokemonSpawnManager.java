package dev.zawarudo.holo.commands.games.pokemon;

import dev.zawarudo.holo.commands.CommandModule;
import dev.zawarudo.holo.core.GuildConfigManager;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.database.dao.PokemonDao;
import dev.zawarudo.holo.modules.pokemon.PokeApiClient;
import dev.zawarudo.holo.modules.pokemon.model.Pokemon;
import dev.zawarudo.holo.modules.pokemon.model.PokemonSpecies;
import dev.zawarudo.holo.utils.ImageOperations;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidIdException;
import dev.zawarudo.holo.utils.exceptions.NotFoundException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Manages the wild {@link Pokemon} in each spawn channel: spawning, guessing, catching, fleeing,
 * and restoring them after a restart. A periodic sweep spawns into empty channels and lets
 * uncaught Pokemon flee.
 */
public class PokemonSpawnManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PokemonSpawnManager.class);

    public static final String CATCH_BUTTON_ID = "pokemon:catch";
    public static final int MAX_WRONG_GUESSES = 3;

    private static final Duration FLEE_AFTER = Duration.ofHours(6);
    private static final Duration REVEAL_VISIBLE_FOR = Duration.ofMinutes(2);
    private static final long SWEEP_INTERVAL_SECONDS = 60;
    /** A legendary or mythical Pokemon is kept 1 in this many times, otherwise it is re-rolled. */
    private static final int LEGENDARY_KEEP_ONE_IN = 10;
    private static final int SHINY_ONE_IN = 512;
    private static final Color SHINY_COLOR = new Color(255, 215, 0);

    /** A wild Pokemon waiting in a channel, with the number of wrong guesses per user. */
    public record WildPokemon(long channelId, long guildId, long messageId, Pokemon pokemon, PokemonSpecies species,
                              boolean shiny, long spawnedAt, Map<Long, Integer> wrongGuesses) {

        WildPokemon(long channelId, long guildId, long messageId, Pokemon pokemon, PokemonSpecies species, boolean shiny, long spawnedAt) {
            this(channelId, guildId, messageId, pokemon, species, shiny, spawnedAt, new ConcurrentHashMap<>());
        }

        public String name() {
            return species.getName("en");
        }

        public int points() {
            return PokemonUtils.catchPoints(species.isLegendary() || species.isMythical(), shiny);
        }
    }

    /** Outcome of a guess. */
    public sealed interface GuessResult {
        /** The Pokemon was caught or fled before the guess arrived. */
        record Gone() implements GuessResult {
        }

        record NoGuessesLeft() implements GuessResult {
        }

        record Wrong(int guessesLeft) implements GuessResult {
        }

        record Caught(WildPokemon pokemon) implements GuessResult {
        }
    }

    private final PokemonDao dao;
    private final GuildConfigManager guildConfigManager;
    private final JDA jda;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService io;

    /** Spawn channel ids mapped to their guild ids. */
    private final Map<Long, Long> channels = new ConcurrentHashMap<>();
    /** Spawn channel ids mapped to the wild Pokemon waiting there. */
    private final Map<Long, WildPokemon> wild = new ConcurrentHashMap<>();
    /** Channels with a spawn in progress, so the sweep and a catch can't spawn twice. */
    private final Set<Long> spawning = ConcurrentHashMap.newKeySet();

    public PokemonSpawnManager(PokemonDao dao, GuildConfigManager guildConfigManager, JDA jda,
                               ScheduledExecutorService scheduler, ExecutorService io) {
        this.dao = dao;
        this.guildConfigManager = guildConfigManager;
        this.jda = jda;
        this.scheduler = scheduler;
        this.io = io;
    }

    /**
     * Restores the spawn channels and their waiting Pokemon, then starts the periodic sweep. Call once at startup.
     */
    public void start() {
        io.execute(this::restoreAll);
        scheduler.scheduleWithFixedDelay(this::safeSweep, SWEEP_INTERVAL_SECONDS, SWEEP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Registers the channel for spawns and spawns its first Pokemon.
     *
     * @return {@code false} if the channel was already a spawn channel.
     */
    public boolean addChannel(@NotNull TextChannel channel) throws SQLException {
        long channelId = channel.getIdLong();
        dao.insertSpawn(channelId, channel.getGuild().getIdLong());
        if (channels.putIfAbsent(channelId, channel.getGuild().getIdLong()) != null) {
            return false;
        }
        spawn(channelId, null);
        return true;
    }

    /**
     * Stops spawns in the channel and deletes its waiting Pokemon.
     *
     * @return {@code false} if the channel wasn't a spawn channel.
     */
    public boolean removeChannel(long channelId) throws SQLException {
        if (channels.remove(channelId) == null) {
            return false;
        }
        dao.deleteSpawn(channelId);
        WildPokemon old = wild.remove(channelId);
        if (old != null) {
            deleteMessage(old);
        }
        return true;
    }

    public boolean isSpawnChannel(long channelId) {
        return channels.containsKey(channelId);
    }

    public List<Long> getChannels(long guildId) {
        return channels.entrySet().stream()
            .filter(e -> e.getValue() == guildId)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
    }

    public boolean isEnabled(@NotNull Guild guild) {
        return guildConfigManager.getOrCreate(guild).isModuleEnabled(CommandModule.ModuleId.POKEMON);
    }

    /**
     * Replaces the waiting Pokemon in a spawn channel with the given one, or a random one if {@code null}.
     */
    public void forceSpawn(long channelId, @Nullable Pokemon pokemon) {
        WildPokemon old = wild.remove(channelId);
        if (old != null) {
            deleteMessage(old);
        }
        spawn(channelId, pokemon);
    }

    public Optional<WildPokemon> getWild(long channelId) {
        return Optional.ofNullable(wild.get(channelId));
    }

    public int guessesLeft(@NotNull WildPokemon pokemon, long userId) {
        return MAX_WRONG_GUESSES - pokemon.wrongGuesses().getOrDefault(userId, 0);
    }

    /**
     * Checks a guess for the Pokemon shown in the given spawn message. Only the first correct guess catches it.
     */
    public GuessResult guess(long channelId, long messageId, long userId, @NotNull String guess) {
        WildPokemon current = wild.get(channelId);
        if (current == null || current.messageId() != messageId) {
            return new GuessResult.Gone();
        }
        if (guessesLeft(current, userId) <= 0) {
            return new GuessResult.NoGuessesLeft();
        }

        Set<String> names = PokemonUtils.nameKeys(current.species().getName("en"), current.species().getName("de"));
        if (!names.contains(PokemonUtils.normalizeName(guess))) {
            int wrong = current.wrongGuesses().merge(userId, 1, Integer::sum);
            return new GuessResult.Wrong(MAX_WRONG_GUESSES - wrong);
        }
        if (!wild.remove(channelId, current)) {
            return new GuessResult.Gone();
        }
        return new GuessResult.Caught(current);
    }

    /**
     * Saves the catch, reveals the Pokemon in its spawn message, and spawns the next one. Blocks on
     * the database and PokeAPI, so call it off the event thread.
     */
    public void completeCatch(@NotNull WildPokemon caught, @NotNull User catcher, @NotNull String catcherName) {
        int points = caught.points();
        long total = points;
        try {
            dao.clearSpawn(caught.channelId());
            dao.insertCatch(catcher.getIdLong(), caught.guildId(), caught.species().getPokedexId(), caught.name(),
                caught.shiny(), points, System.currentTimeMillis());
            total = dao.findPoints(caught.guildId(), catcher.getIdLong());
        } catch (SQLException e) {
            LOGGER.error("Failed to save the catch of {} by user id={}", caught.name(), catcher.getIdLong(), e);
        }

        EmbedBuilder builder = revealEmbed(caught, caught.shiny() ? "✨ Shiny %s caught! ✨" : "The wild %s has been caught!");
        StringBuilder description = new StringBuilder();
        if (caught.species().isLegendary()) {
            description.append("This was a legendary Pokémon!\n");
        } else if (caught.species().isMythical()) {
            description.append("This was a mythical Pokémon!\n");
        }
        if (caught.shiny()) {
            description.append("This was a shiny Pokémon!");
        }
        builder.setDescription(description.toString().strip());
        builder.setFooter(String.format("Caught by %s • +%d %s (server total: %d)", catcherName, points,
            points == 1 ? "point" : "points", total), catcher.getEffectiveAvatarUrl());
        reveal(caught, builder);
        spawn(caught.channelId(), null);
    }

    private void flee(WildPokemon fled) {
        // Caught or replaced in the meantime
        if (!wild.remove(fled.channelId(), fled)) {
            return;
        }
        try {
            dao.clearSpawn(fled.channelId());
        } catch (SQLException e) {
            LOGGER.error("Failed to clear the spawn in channel id={}", fled.channelId(), e);
        }
        reveal(fled, revealEmbed(fled, fled.shiny() ? "✨ The shiny %s fled... ✨" : "The wild %s fled!"));
        spawn(fled.channelId(), null);
    }

    /**
     * Spawns a Pokemon in the channel: the given one, or a random one if {@code null}. A random
     * spawn is skipped if a Pokemon is already waiting there.
     */
    private void spawn(long channelId, @Nullable Pokemon forced) {
        if (!spawning.add(channelId)) {
            return;
        }
        try {
            if (forced == null && wild.containsKey(channelId)) {
                return;
            }
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                LOGGER.warn("Pokémon spawn channel id={} is not accessible; skipping spawn.", channelId);
                return;
            }

            PokemonSpecies species = forced != null ? forced.getPokemonSpecies() : randomSpecies();
            Pokemon pokemon = forced != null ? forced : species.getPokemon();
            boolean shiny = ThreadLocalRandom.current().nextInt(SHINY_ONE_IN) == 0;

            BufferedImage image = PokemonUtils.drawHiddenPokemon(pokemon);
            EmbedBuilder builder = new EmbedBuilder()
                .setTitle("A wild Pokémon appeared!")
                .setDescription("Press **Catch** and type its name!")
                .setImage("attachment://pokemon.png")
                .setColor(EmbedColor.POKEMON.getColor());

            Message msg = channel.sendFiles(FileUpload.fromData(ImageOperations.toInputStream(image), "pokemon.png"))
                .setEmbeds(builder.build())
                .addComponents(ActionRow.of(Button.primary(CATCH_BUTTON_ID, "Catch")))
                .complete();

            long now = System.currentTimeMillis();
            wild.put(channelId, new WildPokemon(channelId, channel.getGuild().getIdLong(), msg.getIdLong(), pokemon, species, shiny, now));
            dao.updateSpawn(channelId, pokemon.getPokedexId(), shiny, msg.getIdLong(), now);
        } catch (APIException | NotFoundException | IOException | SQLException e) {
            LOGGER.error("Failed to spawn a Pokémon in channel id={}", channelId, e);
        } finally {
            spawning.remove(channelId);
        }
    }

    private static PokemonSpecies randomSpecies() throws APIException {
        PokemonSpecies species = PokeApiClient.getRandomPokemonSpecies();
        while ((species.isLegendary() || species.isMythical())
            && ThreadLocalRandom.current().nextInt(LEGENDARY_KEEP_ONE_IN) != 0) {
            species = PokeApiClient.getRandomPokemonSpecies();
        }
        return species;
    }

    private EmbedBuilder revealEmbed(WildPokemon pokemon, String titleFormat) {
        Pokemon.Sprites.Artwork artwork = pokemon.pokemon().getSprites().getOther().getArtwork();
        String image = pokemon.shiny() && artwork.getFrontShiny() != null ? artwork.getFrontShiny() : artwork.getFrontDefault();
        return new EmbedBuilder()
            .setTitle(String.format(titleFormat, pokemon.name()))
            .setImage(image)
            .setColor(pokemon.shiny() ? SHINY_COLOR : EmbedColor.POKEMON.getColor());
    }

    /** Replaces the silhouette and Catch button with the reveal, then deletes the message after a while. */
    private void reveal(WildPokemon pokemon, EmbedBuilder builder) {
        TextChannel channel = jda.getTextChannelById(pokemon.channelId());
        if (channel == null) {
            return;
        }
        channel.editMessageById(pokemon.messageId(), new MessageEditBuilder()
                .setEmbeds(builder.build())
                .setComponents(List.of())
                .setAttachments(List.of())
                .build())
            .queue(
                msg -> msg.delete().queueAfter(REVEAL_VISIBLE_FOR.toSeconds(), TimeUnit.SECONDS, null, _ -> {
                }),
                err -> LOGGER.warn("Failed to reveal the Pokémon in channel id={}", pokemon.channelId(), err)
            );
    }

    private void deleteMessage(WildPokemon pokemon) {
        TextChannel channel = jda.getTextChannelById(pokemon.channelId());
        if (channel != null) {
            channel.deleteMessageById(pokemon.messageId()).queue(null, _ -> {
            });
        }
    }

    private void restoreAll() {
        List<PokemonDao.SpawnRow> rows;
        try {
            rows = dao.findAllSpawns();
        } catch (SQLException e) {
            LOGGER.error("Failed to load the Pokémon spawn channels", e);
            return;
        }
        for (PokemonDao.SpawnRow row : rows) {
            try {
                restore(row);
            } catch (Exception e) {
                LOGGER.error("Failed to restore the Pokémon in channel id={}", row.channelId(), e);
            }
            // Registered only now, so the sweep can't spawn over a Pokemon that is still being restored
            channels.put(row.channelId(), row.guildId());
        }
    }

    private void restore(PokemonDao.SpawnRow row) throws APIException, NotFoundException, InvalidIdException {
        TextChannel channel = jda.getTextChannelById(row.channelId());
        if (channel == null || !row.hasPokemon()) {
            return;
        }
        try {
            channel.retrieveMessageById(row.messageId()).complete();
        } catch (ErrorResponseException _) {
            // The spawn message was deleted while the bot was offline; the sweep spawns a new one
            return;
        }
        Pokemon pokemon = PokeApiClient.getPokemon(row.pokemonId());
        wild.put(row.channelId(), new WildPokemon(row.channelId(), row.guildId(), row.messageId(), pokemon,
            pokemon.getPokemonSpecies(), row.shiny(), row.spawnedAt()));
    }

    private void safeSweep() {
        try {
            sweep();
        } catch (Exception e) {
            LOGGER.error("Pokémon spawn sweep failed", e);
        }
    }

    private void sweep() {
        long now = System.currentTimeMillis();
        channels.forEach((channelId, guildId) -> {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null || !isEnabled(guild)) {
                return;
            }
            WildPokemon current = wild.get(channelId);
            if (current == null) {
                io.execute(() -> spawn(channelId, null));
            } else if (now - current.spawnedAt() >= FLEE_AFTER.toMillis()) {
                io.execute(() -> flee(current));
            }
        });
    }
}
