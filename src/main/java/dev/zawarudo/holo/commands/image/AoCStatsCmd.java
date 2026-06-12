package dev.zawarudo.holo.commands.image;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.aoc.graph.AdventOfCodeGraph;
import dev.zawarudo.holo.modules.aoc.graph.ChartType;
import dev.zawarudo.holo.utils.DateTimeUtils;
import dev.zawarudo.holo.utils.ImageOperations;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.exceptions.APIException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@CommandInfo(name = "aoc",
    description = "Displays the graph of Advent of Code",
    category = CommandCategory.IMAGE)
public class AoCStatsCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AoCStatsCmd.class);

    private static final int LEADERBOARD_ID = 1501119;

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();
        ctx.reply().typing();

        int year = getYear(ctx);

        String token = Bootstrap.holo.getConfig().getAocToken();
        AdventOfCodeGraph graph = AdventOfCodeGraph.createGraph(ChartType.STACKED_BAR_CHART, year, LEADERBOARD_ID, token);

        BufferedImage image;
        try {
            image = graph.generateImage();
        } catch (APIException _) {
            ctx.reply().errorEmbed("Something went wrong while fetching the AOC data. Please try again later.");
            return;
        }

        String name = String.format("aoc_%s.png", DateTimeUtils.getCurrentDateTimeString());

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(String.format("Advent of Code %d Stats", year));
        builder.setImage("attachment://" + name);
        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl()));

        try (InputStream input = ImageOperations.toInputStream(image)) {
            FileUpload upload = FileUpload.fromData(input, name);
            ctx.channel().sendFiles(upload).setEmbeds(builder.build()).queue();
        } catch (IOException ex) {
            ctx.reply().errorEmbed("An error occurred while sending the image. Please try again later.");
            LOGGER.error("An error occurred while sending the AoC image.", ex);
        }
    }

    private int getYear(CommandContext ctx) {
        ZonedDateTime current = ZonedDateTime.now(ZoneId.of("Europe/Zurich"));
        int currentYear = current.getYear();

        if (ctx.hasArgs()) {
            try {
                int parsedYear = Integer.parseInt(ctx.args().getFirst());
                return (parsedYear >= 2015 && parsedYear <= currentYear) ? parsedYear : currentYear;
            } catch (NumberFormatException _) {
                // Fall through to default year logic
            }
        }

        return current.getMonthValue() == 12
            ? current.getYear()
            : current.getYear() - 1;
    }
}
