package dev.zawarudo.holo.commands.games;

import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.minecraft.RconClient;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.annotations.Deactivated;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Deactivated
@CommandInfo(name = "mce2tps",
    description = "Shows the live TPS/MSPT of the MC Eternal 2 server via spark.",
    category = CommandCategory.GAMES)
public class Mce2TpsCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mce2TpsCmd.class);

    private static final String HOST = "cronos.pterodactyl.anachronis.dev";

    private final RconClient rconClient = new RconClient();

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.reply().typing();

        String password = Bootstrap.holo.getConfig().getMce2RconPassword();
        if (password.isBlank()) {
            ctx.reply().errorEmbed("RCON is not configured for the MC Eternal 2 server.", false);
            return;
        }
        int port = Bootstrap.holo.getConfig().getMce2RconPort();

        String response;
        try {
            response = rconClient.sendCommand(HOST, port, password, "spark tps");
        } catch (IOException | IllegalArgumentException ex) {
            ctx.reply().errorEmbed("Could not reach the MC Eternal 2 server's RCON. It may be offline.", false);
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Failed to run 'spark tps' over RCON.", ex);
            }
            return;
        }

        if (response.isBlank()) {
            ctx.reply().errorEmbed("spark did not return any data. Is it installed on the server?", false);
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("MC Eternal 2 - spark TPS/MSPT");
        builder.setColor(getEmbedColor());
        builder.setDescription("```\n" + stripColorCodes(response) + "\n```");
        ctx.reply().embed(builder);
        ctx.channel().sendMessageEmbeds(builder.build()).queue();
    }

    /**
     * spark's console output uses legacy {@code §}-prefixed color codes, which don't render in Discord.
     */
    private String stripColorCodes(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
}
