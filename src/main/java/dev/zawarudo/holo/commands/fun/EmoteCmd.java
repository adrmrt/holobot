package dev.zawarudo.holo.commands.fun;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.emotes.EmoteManager;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CommandInfo(name = "emote",
    description = "Sends a specified emote in the channel as if you had nitro. It uses a fake profile (also called a webhook message) to show who sent the emote.\n\n You may also just use the emote name with the bot prefix.",
    usage = "<emote_name>",
    example = "kekw",
    category = CommandCategory.IMAGE)
public class EmoteCmd extends AbstractCommand implements ExecutableCommand {

    private final EmoteManager emoteManager;

    public EmoteCmd(EmoteManager emoteManager) {
        this.emoteManager = emoteManager;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        if (ctx.argCount() == 1) {
            sendEmote(ctx);
        } else if (ctx.argCount() == 2 && ctx.args().getFirst().equals("search")) {
            searchEmote(ctx);
        } else if (ctx.argCount() == 3 && ctx.args().getFirst().equals("rename")) {
            renameEmote(ctx);
        }
    }

    private void sendEmote(CommandContext ctx) {
        if (ctx.member().isEmpty()) return;

        String emoteName = ctx.args().getFirst();

        try {
            Optional<CustomEmoji> emojiOptional = emoteManager.getEmoteByName(emoteName);

            if (emojiOptional.isEmpty()) {
                ctx.message().ifPresent(m -> m.reply(String.format("Emote not found: %s", emoteName)).queue());
                return;
            }

            ctx.invocation().deleteInvokeIfPossible();
            try {
                Webhook webhook = getWebhook(ctx.channel() instanceof TextChannel tc ? tc : null, ctx.member().orElseThrow());
                webhook.sendMessage(emojiOptional.get().getImageUrl()).queue(m -> webhook.delete().queue());
            } catch (IOException e) {
                ctx.channel().sendMessage(e.getMessage()).queue();
            }
        } catch (SQLException e) {
            ctx.channel().sendMessage(e.getMessage()).queue();
        }
    }

    private void searchEmote(CommandContext ctx) {
        if (!ctx.isBotOwner()) return;

        String keyword = ctx.args().get(1);

        try {
            List<CustomEmoji> emotes = emoteManager.searchEmotesByName(keyword);

            if (emotes.isEmpty()) {
                ctx.message().ifPresent(m -> m.reply(String.format("No emotes found for searched name: %s", keyword)).queue());
                return;
            }

            // TODO: Find better way to display emote search results
            String resultsMessage = emotes.stream().map(CustomEmoji::getName).collect(Collectors.joining("\n"));
            List<String> chunks = splitMessage(resultsMessage);

            for (String chunk : chunks) {
                ctx.message().ifPresent(m -> m.reply(chunk).queue(msg -> msg.delete().queueAfter(5, TimeUnit.MINUTES)));
            }
        } catch (SQLException e) {
            ctx.channel().sendMessage(e.getMessage()).queue();
        }
    }

    private void renameEmote(CommandContext ctx) {
        if (!ctx.isBotOwner()) return;

        String emote = ctx.args().get(1);
        String newName = ctx.args().get(2);

        try {
            emoteManager.renameEmote(emote, newName);
            ctx.message().ifPresent(m -> m.reply(String.format("The emote `%s` has been successfully renamed to `%s`", emote, newName)).queue());
        } catch (SQLException e) {
            ctx.channel().sendMessage(e.getMessage()).queue();
        }
    }

    private Webhook getWebhook(TextChannel channel, @NotNull Member member) throws IOException {
        String avatarUrl = member.getEffectiveAvatarUrl();
        Icon icon = getIcon(avatarUrl);
        return channel.createWebhook(createWebhookId()).setName(member.getEffectiveName()).setAvatar(icon).complete();
    }

    private String createWebhookId() {
        String selfId = Bootstrap.holo.getJDA().getSelfUser().getId();
        return String.format("weebhook-holo-%s", selfId);
    }

    private Icon getIcon(String urlString) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        return Icon.from(connection.getInputStream());
    }

    /**
     * Sends an emote as webhook message.
     */
    public void sendEmoteMessage(MessageReceivedEvent event, CustomEmoji emote) {
        if (event.getMember() == null) {
            return;
        }

        deleteInvoke(event);

        try {
            Webhook webhook = getWebhook(event.getChannel().asTextChannel(), event.getMember());
            webhook.sendMessage(emote.getImageUrl()).queue(m -> webhook.delete().queue());
        } catch (IOException e) {
            event.getChannel().sendMessage(e.getMessage()).queue();
        }
    }

    @Deprecated(forRemoval = true)
    private List<String> splitMessage(String message) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < message.length()) {
            int end = Math.min(start + Message.MAX_CONTENT_LENGTH, message.length());

            if (end < message.length() && message.charAt(end) != '\n') {
                int lastNewline = message.lastIndexOf('\n', end);
                if (lastNewline > start) {
                    end = lastNewline + 1;
                }
            }
            chunks.add(message.substring(start, end));
            start = end;
        }
        return chunks;
    }
}
