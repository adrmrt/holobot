package dev.zawarudo.holo.modules;

import dev.zawarudo.holo.core.misc.Submission;
import org.kohsuke.github.GHIssueBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Locale;

/**
 * Class that handles the connection and interaction with the GitHub repository of the bot.
 */
public class GitHubClient {

    // The repository of the bot
    private final GHRepository repository;

    public GitHubClient(String token) {
        try {
            GitHub gitHub = GitHub.connectUsingOAuth(token);
            repository = gitHub.getRepository("adrmrt/holobot");
        } catch (IOException ex) {
            throw new IllegalStateException("Something went wrong while trying to reach GitHub.", ex);
        }
    }

    /**
     * Creates an issue at the repository of Holo and returns the URL.
     *
     * @param submission The data to be submitted.
     * @return The URL of the issue located in the repository.
     * @throws IOException When something went wrong with the creation of the issue.
     */
    public String createIssue(Submission submission) throws IOException {
        String title = submission.message.substring(0, Math.min(100, submission.message.length()));
        String descriptionTemplate = "# %s\n## Information\n* **Date**: %s\n* **User**: %s (%d)\n* **Guild:** %s (%d)\n* **Channel:** %s (%d)\n## Description\n%s";

        String description = String.format(descriptionTemplate,
            submission.type,
            submission.date,
            submission.author.getName(),
            submission.author.getIdLong(),
            submission.guild.getName(),
            submission.guild.getIdLong(),
            submission.channel.getName(),
            submission.channel.getIdLong(),
            submission.message
        );

        GHIssueBuilder issueBuilder = repository.createIssue(title)
            .body(description)
            .label(submission.type.toLowerCase(Locale.UK));
        return issueBuilder.create().getHtmlUrl().toString();
    }
}
