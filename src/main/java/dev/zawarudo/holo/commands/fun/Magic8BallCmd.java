package dev.zawarudo.holo.commands.fun;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.FileUtils;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

@CommandInfo(name = "8ball",
    description = "Ask the Magic 8 Ball a question and get an answer.",
    usage = "<question>",
    thumbnail = "https://media.discordapp.net/attachments/778991087847079972/946790101109841990/magic8ball.png",
    guildOnly = false,
    category = CommandCategory.MISC)
public class Magic8BallCmd implements CommandMetadata, ExecutableCommand {

    private static final Random RANDOM = new Random();
    private final List<String> responses;

    public Magic8BallCmd() {
        try {
            responses = FileUtils.getAllResourcePaths("image/8ball", "png", "jpg", "jpeg", "webp");
            if (responses.isEmpty()) {
                throw new IllegalStateException("No 8ball images found under classpath: image/8ball");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load 8ball images from resources", e);
        }
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        if (!ctx.hasArgs()) {
            ctx.reply().errorEmbed("Incorrect usage of the command. Please ask a question.");
            return;
        }

        String resourcePath = responses.get(RANDOM.nextInt(responses.size()));
        InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (input == null) {
            ctx.reply().errorEmbed("An error occurred while fetching an answer. Please try again later.");
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Magic 8-Ball");
        builder.setImage("attachment://8ball.png");

        FileUpload upload = FileUpload.fromData(input, "8ball.png");
        ctx.message().ifPresent(m -> m.replyFiles(upload).setEmbeds(builder.build()).queue());
    }
}
