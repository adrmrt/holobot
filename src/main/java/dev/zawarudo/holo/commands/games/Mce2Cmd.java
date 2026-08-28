package dev.zawarudo.holo.commands.games;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.minecraft.MinecraftServerClient;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.lenni0451.mcping.responses.MCPingResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

@CommandInfo(name = "mce2",
    description = "Shows the live status of the MC Eternal 2 server.",
    category = CommandCategory.GAMES)
public class Mce2Cmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mce2Cmd.class);

    private static final String HOST = "cronos.pterodactyl.anachronis.dev";
    private static final int PORT = 25565;
    private static final String PLACEHOLDER_UUID = "00000000-0000-0000-0000-000000000000";

    private final MinecraftServerClient client = new MinecraftServerClient();

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.reply().typing();

        MCPingResponse response;
        try {
            response = client.ping(HOST, PORT);
        } catch (RuntimeException ex) {
            ctx.reply().errorEmbed("Could not reach the MC Eternal 2 server. It may be offline.");
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Failed to ping the MC Eternal 2 server.", ex);
            }
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("MC Eternal 2 Server Status");
        builder.setColor(getEmbedColor());

        String motd = extractMotd(response.description);
        if (!motd.isBlank()) {
            builder.setDescription(motd);
        }

        int online = response.players != null ? response.players.online : 0;
        int max = response.players != null ? response.players.max : 0;
        builder.addField("Players", online + " / " + max, true);
        builder.addField("Version", response.version != null ? response.version.name : "Unknown", true);
        String modded = moddedLabel(response);
        if (modded != null) {
            builder.addField("Modded", modded, true);
        }

        String sample = playerSample(response);
        if (!sample.isEmpty()) {
            builder.addField("Online now", sample, false);
        }

        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl()));
        ctx.channel().sendMessageEmbeds(builder.build()).queue();
    }

    /**
     * Modern servers send the MOTD as a chat-component object (e.g. {@code {"text": "..."}})
     * rather than plain text. Extracts the readable text either way.
     */
    private String extractMotd(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return "";
        }
        try {
            JsonElement element = JsonParser.parseString(rawDescription);
            if (element.isJsonObject() && element.getAsJsonObject().has("text")) {
                return element.getAsJsonObject().get("text").getAsString();
            }
        } catch (JsonSyntaxException ignored) {
            // Not JSON, i.e. already plain text - used as is below.
        }
        return rawDescription;
    }

    private String playerSample(MCPingResponse response) {
        if (response.players == null || response.players.sample == null) {
            return "";
        }
        return Arrays.stream(response.players.sample)
            .filter(p -> !PLACEHOLDER_UUID.equals(p.id))
            .map(p -> p.name)
            .collect(Collectors.joining(", "));
    }

    private String moddedLabel(MCPingResponse response) {
        if (response.forgeData != null) {
            return response.forgeData.mods != null && response.forgeData.mods.length > 0
                ? "Forge (" + response.forgeData.mods.length + " mods)"
                : "Forge";
        }
        if (response.modinfo != null) {
            return "Forge";
        }
        return null;
    }
}
