package dev.zawarudo.holo.commands.fun;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.modules.MerriamWebsterClient;
import dev.zawarudo.holo.utils.Emote;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import dev.zawarudo.holo.utils.exceptions.NotFoundException;
import dev.zawarudo.holo.utils.interact.ButtonPaginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CommandInfo(
    name = "dictionary",
    description = "Looks up a word in the Merriam-Webster Dictionary.",
    usage = "<word>",
    example = "syzygy",
    alias = {"dict", "define"},
    thumbnail = "https://dictionaryapi.com/images/MWLogo.png",
    embedColor = EmbedColor.DICTIONARY,
    category = CommandCategory.MISC
)
public class DictionaryCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryCmd.class);

    private static final int DELETE_AFTER_MINUTES = 5;

    private final ButtonPaginator<MerriamWebsterClient.Entry> paginator;
    private final MerriamWebsterClient client;

    public DictionaryCmd(EventWaiter waiter, MerriamWebsterClient client) {
        this.paginator = new ButtonPaginator<>(
            waiter,
            this::createEmbed,
            "dict",
            DELETE_AFTER_MINUTES, TimeUnit.MINUTES
        );

        this.client = client;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.reply().typing();

        if (!ctx.hasArgs()) {
            ctx.reply().errorEmbed("Please provide a word to look up.");
            return;
        }

        String term = ctx.argString();

        MerriamWebsterClient.LookupResult result;
        try {
            result = client.lookupDictionary(term);
        } catch (APIException | InvalidRequestException | NotFoundException ex) {
            ctx.reply().errorEmbed("An error occurred while looking up that word. Please try again later.");
            LOGGER.error("Dictionary lookup failed: {}", term, ex);
            return;
        }

        if (result.hasEntries()) {
            ctx.message().ifPresent(msg -> paginator.start(msg, ctx.user(), result.entries()));
            return;
        }

        if (result.hasSuggestions()) {
            String suggestions = result.suggestions().stream()
                .limit(10)
                .map(s -> "* " + s)
                .collect(Collectors.joining("\n"));

            MessageEmbed embed = new EmbedBuilder()
                .setThumbnail(getThumbnail())
                .setTitle("Not found")
                .setDescription("Did you mean:\n" + suggestions)
                .setColor(getEmbedColor())
                .build();
            ctx.reply().embedAndDeleteInvoke(ctx, embed, DELETE_AFTER_MINUTES, TimeUnit.MINUTES);
            return;
        }

        MessageEmbed embed = new EmbedBuilder()
            .setThumbnail(getThumbnail())
            .setTitle("Not found")
            .setDescription("No results found for **" + term + "**.")
            .setColor(getEmbedColor())
            .build();
        ctx.reply().embedAndDeleteInvoke(ctx, embed, DELETE_AFTER_MINUTES, TimeUnit.MINUTES);
    }

    private MessageEmbed createEmbed(MerriamWebsterClient.Entry entry, int index, int total) {
        EmbedBuilder b = new EmbedBuilder()
            .setThumbnail(getThumbnail())
            .setColor(getEmbedColor());

        String word = entry.headword() == null ? "Unknown" : entry.headword();
        String fl = entry.functionalLabel();
        String title = (fl == null || fl.isBlank()) ? word : String.format("%s (%s)", word, fl);

        String warning = entry.offensive() ? Emote.WARNING.getAsEmoji().getFormatted() : "";

        b.setTitle(title + " " + warning);

        List<String> defs = entry.shortDefs() == null ? List.of() : entry.shortDefs();
        String description;
        if (defs.isEmpty()) {
            description = "_No definition available._";
        } else if (defs.size() == 1) {
            description = defs.getFirst();
        } else {
            description = defs.stream()
                .map(d -> "• " + d)
                .collect(Collectors.joining("\n"));
        }

        b.setDescription(Formatter.truncate(description, MessageEmbed.DESCRIPTION_MAX_LENGTH));

        if (entry.pronunciation() != null && !entry.pronunciation().isBlank()) {
            b.addField("Pronunciation", entry.pronunciation(), true);
        }

        if (entry.plural() != null && !entry.plural().isBlank()) {
            b.addField("Plural", entry.plural(), true);
        }

        if (entry.etymology() != null && !entry.etymology().isBlank()) {
            String et = Formatter.truncate(entry.etymology(), MessageEmbed.VALUE_MAX_LENGTH);
            b.addField("Etymology", et, false);
        }

        if (entry.usageNotes() != null && !entry.usageNotes().isBlank()) {
            String usage = Formatter.truncate(entry.usageNotes(), MessageEmbed.VALUE_MAX_LENGTH);
            b.addField("Usage note", usage, false);
        }

        List<String> examples = entry.examples();

        if (examples != null && !examples.isEmpty()) {
            String exText = examples.stream()
                .limit(8)
                .map(e -> "• " + e)
                .collect(Collectors.joining("\n"));
            b.addField("Examples", Formatter.truncate(exText, MessageEmbed.VALUE_MAX_LENGTH), false);
        }

        return ButtonPaginator.withPageFooter(b, index, total);
    }
}
