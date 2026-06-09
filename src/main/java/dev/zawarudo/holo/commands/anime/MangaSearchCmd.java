package dev.zawarudo.holo.commands.anime;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.modules.anime.MediaPlatform;
import dev.zawarudo.holo.modules.anime.MediaSearchService;
import dev.zawarudo.holo.modules.anime.MangaResult;
import org.jetbrains.annotations.Nullable;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import dev.zawarudo.holo.utils.interact.ReactionSelector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandInfo(name = "mangasearch",
    description = "Use this command to search for a manga. Optionally specify a platform (mal or anilist) to search on.",
    usage = "[mal|anilist] <title>",
    example = "mal black clover",
    alias = {"ms", "manga"},
    thumbnail = "https://upload.wikimedia.org/wikipedia/commons/7/7a/MyAnimeList_Logo.png",
    embedColor = EmbedColor.MAL,
    category = CommandCategory.ANIME)
public class MangaSearchCmd extends AbstractCommand implements ExecutableCommand {

    private final MediaSearchService searchService;
    private final ReactionSelector<MangaResult> selector;

    public MangaSearchCmd(EventWaiter waiter, MediaSearchService searchService) {
        this.searchService = searchService;

        this.selector = new ReactionSelector<>(
            waiter,
            items -> {
                MediaPlatform platform = items.getFirst().platform();
                return ReactionSelector.defaultNumberedListEmbed(
                    "Manga Search Results",
                    items,
                    m -> String.format("%s [%s]", m.title(), m.type()),
                    getEmbedColor(),
                    platform.getName(),
                    platform.getUrl(),
                    platform.getIconUrl()
                );
            }
        );
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.reply().typing();

        if (!ctx.hasArgs()) {
            ctx.reply().errorEmbed("Please provide a title to search for.");
            return;
        }

        MediaPlatform platform = parsePlatformFlag(ctx.args().getFirst());
        final String search = platform != null
            ? String.join(" ", ctx.args().subList(1, ctx.args().size())).trim()
            : ctx.argString();

        if (search.isBlank()) {
            ctx.reply().errorEmbed("Please provide a title to search for.");
            return;
        }

        final List<MangaResult> results;
        try {
            List<MediaPlatform> order = platform != null ? List.of(platform) : null;
            results = order != null
                ? searchService.searchManga(search, 10, order)
                : searchService.searchManga(search, 10);
        } catch (APIException | InvalidRequestException ex) {
            ctx.reply().errorEmbed("An error occurred while trying to search for the manga! Please try again later.");
            logger.error("Manga search failed: {}", search, ex);
            return;
        }

        if (results.isEmpty()) {
            ctx.reply().errorEmbed("I couldn't find any mangas with your given search terms!");
            return;
        }

        ctx.message().ifPresentOrElse(invokeMessage -> selector.start(invokeMessage, ctx.user(), results, (evt, selected, index) -> {
            EmbedBuilder builder = createEmbed(selected);
            builder.setColor(getEmbedColor());
            ctx.reply().embed(builder);
        }), () -> {
            // TODO: Handle slash command version better
            MangaResult first = results.getFirst();
            EmbedBuilder builder = createEmbed(first);
            builder.setColor(getEmbedColor());
            ctx.reply().embed(builder);
        });
    }

    private EmbedBuilder createEmbed(@NotNull MangaResult manga) {
        EmbedBuilder b = new EmbedBuilder();

        String type = manga.type();
        String title = Formatter.truncate(manga.title(), MessageEmbed.TITLE_MAX_LENGTH);
        b.setTitle(String.format("%s [%s]", title, type));

        if (manga.imageUrl() != null) {
            b.setThumbnail(manga.imageUrl());
        }

        if (manga.synopsis() != null && !manga.synopsis().isBlank()) {
            String synopsis = Formatter.truncate(manga.synopsis(), MessageEmbed.DESCRIPTION_MAX_LENGTH);
            b.setDescription(synopsis);
        }

        // Titles
        if (manga.titleEnglish() != null && !manga.titleEnglish().isBlank()
            && !manga.titleEnglish().equalsIgnoreCase(manga.title())) {
            b.addField("English Title", manga.titleEnglish(), true);
        }
        if (manga.titleJapanese() != null && !manga.titleJapanese().isBlank()) {
            b.addField("Japanese Title", manga.titleJapanese(), true);
        }

        String authors = formatList(manga.authors());
        if (authors != null) {
            b.addField("Author/Mangaka", authors, false);
        }

        String genres = formatList(manga.genres());
        if (genres != null) {
            b.addField("Genres", genres, false);
        }
        String themes = formatList(manga.themes());
        if (themes != null) {
            b.addField("Themes", themes, false);
        }
        String demographics = formatList(manga.demographics());
        if (demographics != null) {
            b.addField("Demographics", demographics, false);
        }

        // Basic info
        if (manga.status() != null && !manga.status().isBlank()) {
            b.addField("Status", manga.status(), true);
        }

        int chapters = manga.chapters();
        int volumes = manga.volumes();

        if ("Light Novel".equals(type)) {
            b.addField("Volumes", formatChapters(chapters, volumes), true);
        } else {
            b.addField("Chapters", formatChapters(chapters, volumes), true);
        }

        b.addBlankField(true);

        // Scores
        String scoreLabel = switch (manga.platform()) {
            case ANILIST -> "AniList Score";
            case MAL_JIKAN -> "MAL Score";
        };
        String scoreValue = manga.score();
        String rankLabel = switch (manga.platform()) {
            case ANILIST -> "AniList Rank";
            case MAL_JIKAN -> "MAL Rank";
        };
        String rankValue = formatRank(manga.rank());

        b.addField(scoreLabel, scoreValue, true);
        b.addField(rankLabel, rankValue, true);
        b.addBlankField(true);

        // Link
        String linkName = (manga.platform() == MediaPlatform.ANILIST)
            ? "AniList"
            : "MyAnimeList";
        if (!manga.url().isBlank()) {
            b.addField("Link", "[" + linkName + "](" + manga.url() + ")", false);
        }

        b.setAuthor(manga.platform().getName(), manga.platform().getUrl(), manga.platform().getIconUrl());
        return b;
    }

    private String formatRank(int rank) {
        return rank == 0 ? "N/A" : String.valueOf(rank);
    }

    private String formatChapters(int ch, int vol) {
        String displayText;
        if (ch != 0) {
            if (vol != 0) {
                displayText = String.format("Vol: %d%nCh: %d", vol, ch);
            } else {
                displayText = String.format("%d Ch.", ch);
            }
        } else {
            displayText = "TBA";
        }
        return displayText;
    }

    private String formatList(List<String> list) {
        if (list.isEmpty()) {
            return null;
        }
        return String.join(", ", list);
    }

    @Nullable
    private static MediaPlatform parsePlatformFlag(String token) {
        return switch (token.toLowerCase()) {
            case "mal", "myanimelist" -> MediaPlatform.MAL_JIKAN;
            case "anilist", "al" -> MediaPlatform.ANILIST;
            default -> null;
        };
    }
}
