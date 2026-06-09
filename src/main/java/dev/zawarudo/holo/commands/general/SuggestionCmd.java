package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.core.misc.Submission;
import dev.zawarudo.holo.modules.GitHubClient;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "suggestion",
    description = "Use this command if you want to suggest a feature. Suggestions are always appreciated.",
    usage = "<text>",
    example = "Make this bot more awesome <3",
    category = CommandCategory.GENERAL)
public class SuggestionCmd extends AbstractCommand implements ExecutableCommand {

    private final GitHubClient githubClient;

    public SuggestionCmd(GitHubClient githubClient) {
        this.githubClient = githubClient;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();
        EmbedBuilder eb = new EmbedBuilder();

        if (!ctx.hasArgs()) {
            eb.setTitle("Incorrect Usage");
            eb.setDescription("Please provide a description of your suggestion");
            ctx.channel().sendMessageEmbeds(eb.build()).queue(msg -> msg.delete().queueAfter(30, TimeUnit.SECONDS, null, ignored -> {
            }));
            return;
        }

        String url;
        try {
            Submission submission = new Submission("Suggestion", ctx, String.join(" ", ctx.args()));
            url = githubClient.createIssue(submission);
        } catch (IOException ex) {
            eb.setTitle("Error");
            eb.setDescription("An error occurred while creating a GitHub ticket! Please try again later.");
            ctx.channel().sendMessageEmbeds(eb.build()).queue(msg -> msg.delete().queueAfter(30, TimeUnit.SECONDS, null, ignored -> {
            }));
            return;
        }

        logger.info("Created a GitHub issue: {}", url);

        eb.setTitle("Suggestion Submitted");
        eb.setDescription("Thank you for your suggestion!");
        ctx.channel().sendMessageEmbeds(eb.build()).queue(msg -> msg.delete().queueAfter(30, TimeUnit.SECONDS, null, ignored -> {
        }));
    }
}
