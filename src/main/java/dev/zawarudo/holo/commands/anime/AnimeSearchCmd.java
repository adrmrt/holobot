package dev.zawarudo.holo.commands.anime;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.modules.anime.MediaPlatform;
import dev.zawarudo.holo.modules.anime.MediaSearchService;
import dev.zawarudo.holo.modules.anime.model.AnimeResult;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import dev.zawarudo.holo.utils.interact.ReactionSelector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandInfo(name = "animesearch",
        description = "Use this command to search for an anime in the database of MyAnimeList.",
        usage = "<title>",
        example = "one piece",
        alias = {"as", "anime"},
        thumbnail = "https://upload.wikimedia.org/wikipedia/commons/7/7a/MyAnimeList_Logo.png",
        embedColor = EmbedColor.MAL,
        category = CommandCategory.ANIME)
public class AnimeSearchCmd extends AbstractCommand implements ExecutableCommand {

    private final MediaSearchService searchService;
    private final ReactionSelector<AnimeResult> selector;

    public AnimeSearchCmd(EventWaiter waiter, MediaSearchService searchService) {
        this.searchService = searchService;

        this.selector = new ReactionSelector<>(
                waiter,
                items -> ReactionSelector.defaultNumberedListEmbed(
                        "Anime Search Results",
                        items,
                        a -> String.format("%s [%s]", a.title(), a.type()),
                        getEmbedColor()
                )
        );
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.reply().typing();

        if (!ctx.hasArgs()) {
            ctx.reply().errorEmbed("Please provide a title to search for.");
            return;
        }

        final String search = ctx.argString();

        final List<AnimeResult> results;
        try {
            results = searchService.searchAnime(search, 10);
        } catch (APIException | InvalidRequestException ex) {
            ctx.reply().errorEmbed("An error occurred while trying to search for the anime! Please try again later.");
            logger.error("Anime search failed: {}", search, ex);
            return;
        }

        if (results.isEmpty()) {
            ctx.reply().errorEmbed("I couldn't find any anime with your given search terms!");
            return;
        }

        ctx.message().ifPresentOrElse(invokeMessage -> selector.start(invokeMessage, ctx.user(), results, (evt, selected, index) -> {
            EmbedBuilder builder = createEmbed(selected);
            builder.setColor(getEmbedColor());
            ctx.reply().embed(builder);
        }), () -> {
            // TODO: Handle slash command version better
            AnimeResult first = results.getFirst();
            EmbedBuilder builder = createEmbed(first);
            builder.setColor(getEmbedColor());
            ctx.reply().embed(builder);
        });
    }

    private EmbedBuilder createEmbed(@NotNull AnimeResult anime) {
        EmbedBuilder b = new EmbedBuilder();

        String type = anime.type();
        String title = Formatter.truncate(anime.title(), MessageEmbed.TITLE_MAX_LENGTH);
        b.setTitle(String.format("%s [%s]", title, type));

        if (anime.imageUrl() != null) {
            b.setThumbnail(anime.imageUrl());
        }

        if (anime.synopsis() != null && !anime.synopsis().isBlank()) {
            String synopsis = Formatter.truncate(anime.synopsis(), MessageEmbed.DESCRIPTION_MAX_LENGTH);
            b.setDescription(synopsis);
        }

        // Titles
        if (anime.titleEnglish() != null && !anime.titleEnglish().isBlank()
                && !anime.titleEnglish().equalsIgnoreCase(anime.title())) {
            b.addField("English Title", anime.titleEnglish(), true);
        }
        if (anime.titleJapanese() != null && !anime.titleJapanese().isBlank()) {
            b.addField("Japanese Title", anime.titleJapanese(), true);
        }

        String studios = formatList(anime.studios());
        if (studios != null) {
            b.addField("Studio", studios, false);
        }

        String genres = formatList(anime.genres());
        if (genres != null) {
            b.addField("Genres", genres, false);
        }
        String themes = formatList(anime.themes());
        if (themes != null) {
            b.addField("Themes", themes, false);
        }
        String demographics = formatList(anime.demographics());
        if (demographics != null) {
            b.addField("Demographics", demographics, false);
        }

        // Basic info
        if (anime.status() != null && !anime.status().isBlank()) {
            b.addField("Status", anime.status(), true);
        }

        if ("Movie".equalsIgnoreCase(type)) {
            b.addField("Season", formatSeason(anime.season()), true);
            b.addBlankField(true);
        } else {
            b.addField("Episodes", formatEpisodes(anime.episodes()), true);
            b.addField("Season", formatSeason(anime.season()), true);
        }

        // Scores
        String scoreLabel = switch (anime.platform()) {
            case ANILIST -> "AniList Score";
            case MAL_JIKAN -> "MAL Score";
        };
        String scoreValue = formatScore(anime.score());
        String rankLabel = switch (anime.platform()) {
            case ANILIST -> "AniList Rank";
            case MAL_JIKAN -> "MAL Rank";
        };
        String rankValue = formatRank(anime.rank());

        b.addField(scoreLabel, scoreValue, true);
        b.addField(rankLabel, rankValue, true);
        b.addBlankField(true);

        // Link
        String linkName = (anime.platform() == MediaPlatform.ANILIST)
                ? "AniList"
                : "MyAnimeList";
        if (!anime.url().isBlank()) {
            b.addField("Link", "[" + linkName + "](" + anime.url() + ")", false);
        }

        b.setAuthor(anime.platform().getName(), anime.platform().getUrl(), anime.platform().getIconUrl());
        return b;
    }

    private String formatEpisodes(int episodes) {
        return episodes == 0 ? "TBA" : String.valueOf(episodes);
    }

    private String formatScore(double score) {
        return score == 0.0 ? "N/A" : String.valueOf(score);
    }

    private String formatRank(int rank) {
        return rank == 0 ? "N/A" : String.valueOf(rank);
    }

    private String formatSeason(String season) {
        if (season == null || season.isBlank()) return "TBA";
        return Formatter.capitalize(season);
    }

    private String formatList(List<String> list) {
        if (list.isEmpty()) {
            return null;
        }
        return String.join(", ", list);
    }
}