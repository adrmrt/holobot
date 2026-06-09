package dev.zawarudo.holo.commands.owner;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@CommandInfo(
    name = "purge",
    description = "Deletes messages from the last N messages in this channel (self purge).",
    usage = "<amount>",
    ownerOnly = true,
    category = CommandCategory.OWNER
)
public class PurgeCmd extends AbstractCommand implements ExecutableCommand {

    private static final int PAGE_SIZE = 100;
    private static final Duration BULK_MAX_AGE = Duration.ofDays(14);

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        if (!ctx.hasArgs()) return;

        boolean botMode = false;
        int amount;

        if ("bot".equalsIgnoreCase(ctx.args().getFirst())) {
            botMode = true;
            if (ctx.argCount() < 2) return;

            if (!ctx.isBotOwner()) {
                return;
            }

            amount = parseInt(ctx.args().get(1));
        } else {
            amount = parseInt(ctx.args().getFirst());
        }

        if (amount <= 0) return;

        MessageChannel channel = ctx.channel();
        long targetAuthorId = botMode
            ? ctx.jda().getSelfUser().getIdLong()
            : ctx.user().getIdLong();

        // Scan last <amount> messages, delete those authored by target
        List<Message> toDelete = collectMessagesToDelete(channel, amount, targetAuthorId);

        if (toDelete.isEmpty()) return;

        // Bulk delete only works in guild channels, and only for <14d messages
        boolean canBulkDelete = (channel.getType().isGuild() && channel.getType() != ChannelType.GUILD_NEWS_THREAD);
        deleteMixed(channel, toDelete, canBulkDelete);
    }

    private List<Message> collectMessagesToDelete(MessageChannel channel, int scanCount, long authorId) {
        List<Message> matches = new ArrayList<>();

        int remainingToScan = scanCount;
        String beforeId = null;

        while (remainingToScan > 0) {
            int fetch = Math.min(PAGE_SIZE, remainingToScan);

            List<Message> page = (beforeId == null)
                ? channel.getHistory().retrievePast(fetch).complete()
                : channel.getHistoryBefore(beforeId, fetch).complete().getRetrievedHistory();

            if (page.isEmpty()) break;

            beforeId = page.getLast().getId();
            remainingToScan -= page.size();

            for (Message m : page) {
                if (m.getAuthor().getIdLong() == authorId) {
                    matches.add(m);
                }
            }
        }

        return matches;
    }

    private void deleteMixed(MessageChannel channel, List<Message> messages, boolean canBulkDelete) {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(BULK_MAX_AGE);

        List<Message> bulkable = new ArrayList<>();
        List<Message> tooOld = new ArrayList<>();

        for (Message m : messages) {
            if (m.getTimeCreated().isAfter(cutoff)) bulkable.add(m);
            else tooOld.add(m);
        }

        // Bulk delete in chunks (guild channels only)
        if (canBulkDelete && channel instanceof GuildMessageChannel guildChannel) {
            for (int i = 0; i < bulkable.size(); i += PAGE_SIZE) {
                List<Message> chunk = bulkable.subList(i, Math.min(i + PAGE_SIZE, bulkable.size()));
                if (chunk.size() == 1) {
                    chunk.getFirst().delete().queue();
                } else {
                    guildChannel.deleteMessages(chunk).queue();
                }
            }
        } else {
            // Non-guild channels: delete individually
            for (Message m : bulkable) {
                m.delete().queue();
            }
        }

        // Old messages must be deleted individually everywhere
        for (Message m : tooOld) {
            m.delete().queue();
        }
    }
}
