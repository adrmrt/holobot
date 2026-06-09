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

@CommandInfo(name = "bug",
    description = "Use this command to report a bug. Please provide a description of the bug and how it happened.",
    usage = "<text>",
    example = "Something went wrong",
    category = CommandCategory.GENERAL)
public class BugCmd extends AbstractCommand implements ExecutableCommand {

    private final GitHubClient githubClient;

    public BugCmd(GitHubClient githubClient) {
        this.githubClient = githubClient;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();
        EmbedBuilder eb = new EmbedBuilder();

        if (!ctx.hasArgs()) {
            eb.setTitle("Incorrect Usage");
            eb.setDescription("Please provide a description of the bug");
            ctx.channel().sendMessageEmbeds(eb.build()).queue(msg -> msg.delete().queueAfter(30, TimeUnit.SECONDS, null, ignored -> {
            }));
            return;
        }

        String url;
        try {
            Submission submission = new Submission("Bug", ctx, String.join(" ", ctx.args()));
            url = githubClient.createIssue(submission);
        } catch (IOException ex) {
            eb.setTitle("Error");
            eb.setDescription("An error occurred while creating a GitHub ticket! Please try again later.");
            ctx.channel().sendMessageEmbeds(eb.build()).queue(msg -> msg.delete().queueAfter(30, TimeUnit.SECONDS, null, ignored -> {
            }));
            return;
        }

        logger.info("Created a GitHub issue: {}", url);

        eb.setTitle("Bug Report Submitted");
        eb.setDescription("Thank you for reporting this bug! We will review it as soon as possible.");
        ctx.channel().sendMessageEmbeds(eb.build()).queue(msg -> msg.delete().queueAfter(30, TimeUnit.SECONDS, null, ignored -> {
        }));
    }
}
