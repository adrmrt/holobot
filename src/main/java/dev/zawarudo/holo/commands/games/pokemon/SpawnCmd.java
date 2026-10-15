package dev.zawarudo.holo.commands.games.pokemon;

import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.modules.pokemon.PokeApiClient;
import dev.zawarudo.holo.modules.pokemon.model.Pokemon;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.exceptions.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CommandInfo(name = "spawn",
    description = "Manages the channels where wild Pokémon spawn. The bot owner can also replace the " +
        "Pokémon in this channel with a random or specific one.",
    usage = "add | remove | list | [<Pokémon name or id> | random]",
    adminOnly = true,
    embedColor = EmbedColor.POKEMON,
    category = CommandCategory.GAMES)
public class SpawnCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnCmd.class);

    private final PokemonSpawnManager pokemonSpawnManager;

    public SpawnCmd(PokemonSpawnManager pokemonSpawnManager) {
        this.pokemonSpawnManager = pokemonSpawnManager;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        String subcommand = ctx.arg(0).map(s -> s.toLowerCase(Locale.ROOT)).orElse("random");
        try {
            switch (subcommand) {
                case "add" -> add(ctx);
                case "remove" -> remove(ctx);
                case "list" -> list(ctx);
                default -> force(ctx);
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to update the Pokémon spawn channels", e);
            ctx.reply().errorEmbed("Something went wrong while saving the spawn channels. Please try again later.");
        }
    }

    private void add(CommandContext ctx) throws SQLException {
        if (ctx.channel().getType() != ChannelType.TEXT) {
            ctx.reply().errorEmbed("Pokémon can only spawn in regular text channels.");
            return;
        }
        boolean added = pokemonSpawnManager.addChannel(ctx.channel().asTextChannel());
        confirm(ctx, added ? "Wild Pokémon will now spawn in this channel." : "This channel is already a spawn channel.");
    }

    private void remove(CommandContext ctx) throws SQLException {
        boolean removed = pokemonSpawnManager.removeChannel(ctx.channel().getIdLong());
        confirm(ctx, removed ? "Wild Pokémon will no longer spawn in this channel." : "This channel isn't a spawn channel.");
    }

    private void list(CommandContext ctx) {
        List<Long> channels = pokemonSpawnManager.getChannels(ctx.guildIdOrZero());
        confirm(ctx, channels.isEmpty()
            ? "There are no spawn channels yet. Use `spawn add` in a channel to add it."
            : "Wild Pokémon spawn in:\n" + channels.stream().map(id -> "<#" + id + ">").collect(Collectors.joining("\n")));
    }

    private void force(CommandContext ctx) {
        if (!ctx.isBotOwner()) {
            ctx.reply().errorEmbed("Only the bot owner can force a Pokémon to spawn.");
            return;
        }
        long channelId = ctx.channel().getIdLong();
        if (!pokemonSpawnManager.isSpawnChannel(channelId)) {
            ctx.reply().errorEmbed("This channel isn't a spawn channel. Use `spawn add` first.");
            return;
        }

        String search = ctx.hasArgs() ? ctx.argString() : "random";
        try {
            Pokemon pokemon;
            if ("random".equalsIgnoreCase(search)) {
                pokemon = null;
            } else if (isNumeric(search)) {
                pokemon = PokeApiClient.getPokemon(Integer.parseInt(search));
            } else {
                // Species lookup, so names like "deoxys" resolve to their default form
                pokemon = PokeApiClient.getPokemonSpecies(search).getPokemon();
            }
            pokemonSpawnManager.forceSpawn(channelId, pokemon);
        } catch (APIException _) {
            sendOwnerError(ctx, "PokéAPI error right now. Try again later.");
        } catch (NotFoundException | InvalidIdException _) {
            sendOwnerError(ctx, "Pokémon not found. Check typos / ID.");
        }
    }

    private void confirm(CommandContext ctx, String text) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setDescription(text);
        builder.setColor(getEmbedColor());
        ctx.reply().embed(builder.build(), 30, TimeUnit.SECONDS);
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
