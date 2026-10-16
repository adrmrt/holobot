package dev.zawarudo.holo.commands.games.pokemon;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.database.dao.PokemonDao;
import dev.zawarudo.holo.database.dao.PokemonDao.CollectionEntry;
import dev.zawarudo.holo.modules.pokemon.PokeApiClient;
import dev.zawarudo.holo.utils.UserResolver;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.interact.ButtonPaginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CommandInfo(name = "pc",
    description = "Shows the Pokémon you or another user have caught.",
    usage = "[<user>]",
    embedColor = EmbedColor.POKEMON,
    guildOnly = false,
    category = CommandCategory.GAMES)
public class PcCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PcCmd.class);
    private static final int PAGE_SIZE = 20;

    /** One page of a user's collection, with the summary shown on every page. */
    private record Page(User owner, String summary, List<CollectionEntry> entries) {
    }

    private final PokemonDao dao;
    private final UserResolver userResolver;
    private final ButtonPaginator<Page> paginator;

    public PcCmd(PokemonDao dao, UserResolver userResolver, EventWaiter waiter) {
        this.dao = dao;
        this.userResolver = userResolver;
        this.paginator = new ButtonPaginator<>(waiter, this::render, "pc", 5, TimeUnit.MINUTES);
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        User owner = userResolver.resolveUser(ctx).orElse(ctx.user());

        List<CollectionEntry> entries;
        try {
            entries = dao.findCollection(owner.getIdLong());
        } catch (SQLException e) {
            LOGGER.error("Failed to load the Pokémon collection of user id={}", owner.getIdLong(), e);
            ctx.reply().errorEmbed("Couldn't load the collection. Please try again later.");
            return;
        }

        if (entries.isEmpty()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setDescription(owner.getEffectiveName() + " hasn't caught any Pokémon yet.");
            builder.setColor(getEmbedColor());
            ctx.reply().embed(builder);
            return;
        }

        int catches = entries.stream().mapToInt(CollectionEntry::total).sum();
        int shinies = entries.stream().mapToInt(CollectionEntry::shinies).sum();
        String summary = String.format("**%d/%d** species caught • %d catches%s",
            entries.size(), PokeApiClient.POKEMON_COUNT, catches, shinies > 0 ? " • ✨ " + shinies : "");

        List<Page> pages = new ArrayList<>();
        for (int i = 0; i < entries.size(); i += PAGE_SIZE) {
            pages.add(new Page(owner, summary, entries.subList(i, Math.min(i + PAGE_SIZE, entries.size()))));
        }
        ctx.message().ifPresent(msg -> paginator.start(msg, ctx.user(), pages));
    }

    private MessageEmbed render(Page page, int index, int total) {
        String lines = page.entries().stream()
            .map(e -> String.format("`#%04d` %s ×%d%s", e.speciesId(), e.name(), e.total(), e.shinies() > 0 ? " ✨" : ""))
            .collect(Collectors.joining("\n"));

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(page.owner().getEffectiveName() + "'s PC");
        builder.setDescription(page.summary() + "\n\n" + lines);
        builder.setColor(getEmbedColor());
        return ButtonPaginator.withPageFooter(builder, index, total);
    }
}
