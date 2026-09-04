package dev.zawarudo.holo.commands.games.pokemon;

import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.modules.pokemon.PokeApiClient;
import dev.zawarudo.holo.modules.pokemon.model.Pokemon;
import dev.zawarudo.holo.utils.ImageOperations;
import dev.zawarudo.holo.utils.exceptions.APIException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that manages where and when a {@link Pokemon} spawns.
 */
public class PokemonSpawnManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PokemonSpawnManager.class);

    private final List<Long> channels;

    private final JDA jda;
    /**
     * {@link Pokemon}s mapped to {@link TextChannel}s
     */
    private final Map<Long, Pokemon> pokemon;
    /**
     * {@link Message}s containing the embed with the Pokemon in each {@link TextChannel}
     */
    private final Map<Long, Message> messages;

    public PokemonSpawnManager(JDA jda) {
        this.jda = jda;
        this.channels = new ArrayList<>();
        pokemon = new HashMap<>();
        messages = new HashMap<>();

        spawnPokemon();
    }

    /**
     * Starts spawning Pokemon in the channels.
     */
    public void spawnPokemon() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("A wild Pokémon appeared!");
        builder.setColor(Color.RED);

        for (Long id : channels) {
            Pokemon pokemon;
            FileUpload upload;
            try {
                pokemon = PokeApiClient.getRandomPokemon();
                BufferedImage image = PokemonUtils.drawHiddenPokemon(pokemon);
                upload = FileUpload.fromData(ImageOperations.toInputStream(image), "pokemon.png");
            } catch (APIException ex) {
                LOGGER.error("Failed to fetch random Pokémon for channel id={}", id, ex);
                continue;
            } catch (IOException ex) {
                LOGGER.error("Failed to render Pokémon image for channel id={}", id, ex);
                continue;
            }

            builder.setDescription("Type `" + getPrefix(getChannelFromId(id).getGuild()) + "catch <pokémon name>` to catch it!");
            builder.setImage("attachment://pokemon.png");

            Message msg = getChannelFromId(id).sendFiles(upload).setEmbeds(builder.build()).complete();

            this.pokemon.put(id, pokemon);
            messages.put(id, msg);
        }
    }

    public void spawnNewPokemon(long channelId) {
        Pokemon pokemon;
        try {
            pokemon = PokeApiClient.getRandomPokemon();
            spawnNewPokemon(channelId, pokemon);
        } catch (APIException ex) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Something went wrong while spawning a new Pokémon in the channel with id={}", channelId, ex);
            }
        }
    }

    public void spawnNewPokemon(long channelId, Pokemon pokemon) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("A wild Pokémon appeared!");
        builder.setColor(Color.RED);
        builder.setDescription("Type `" + getPrefix(getChannelFromId(channelId).getGuild()) + "catch <Pokémon name>` to catch it!");

        FileUpload upload;
        try {
            BufferedImage image = PokemonUtils.drawHiddenPokemon(pokemon);
            upload = FileUpload.fromData(ImageOperations.toInputStream(image), "pokemon.png");
        } catch (IOException ex) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Something went wrong while spawning a new Pokémon in the channel with id={}", channelId, ex);
            }
            return;
        }
        builder.setImage("attachment://pokemon.png");

        Message msg = getChannelFromId(channelId).sendFiles(upload).setEmbeds(builder.build()).complete();
        this.pokemon.put(channelId, pokemon);
        messages.put(channelId, msg);
    }

    public void addChannel(long channelId) {
        channels.add(channelId);
        spawnNewPokemon(channelId);
    }

    public void removeChannel(long channelId) {
        if (!channels.contains(channelId)) {
            return;
        }
        channels.remove(channelId);
        pokemon.remove(channelId);
        messages.remove(channelId).delete().queue();
    }

    @NotNull
    public TextChannel getChannelFromId(long id) {
        TextChannel channel = jda.getTextChannelById(id);
        if (channel == null) {
            throw new IllegalArgumentException("TextChannel with id " + id + " does not exist!");
        }
        return channel;
    }

    /**
     * Deletes the message containing the Pokemon in the given {@link TextChannel}
     */
    public void deleteMessage(long channelId) {
        if (messages.containsKey(channelId)) {
            messages.get(channelId).delete().queue();
        }
    }

    /**
     * Returns the {@link Pokemon} in the given {@link TextChannel}
     */
    public Pokemon getPokemon(long channelId) {
        return pokemon.get(channelId);
    }

    /**
     * Returns the {@link Message} containing the Pokemon in the given {@link TextChannel}
     */
    public Message getMessage(long channelId) {
        return messages.get(channelId);
    }

    /**
     * Returns the mapping of {@link TextChannel} IDs to {@link Pokemon}s
     */
    public Map<Long, Pokemon> getPokemon() {
        return pokemon;
    }

    /**
     * Returns the mapping of {@link TextChannel} IDs to {@link Message}s
     */
    public Map<Long, Message> getMessages() {
        return messages;
    }

    private String getPrefix(Guild guild) {
        return Bootstrap.holo.getGuildConfigManager().getOrCreate(guild).getPrefix();
    }
}