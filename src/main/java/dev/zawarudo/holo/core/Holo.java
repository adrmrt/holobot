package dev.zawarudo.holo.core;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import dev.zawarudo.holo.commands.CommandListener;
import dev.zawarudo.holo.commands.CommandManager;
import dev.zawarudo.holo.commands.ModuleRegistry;
import dev.zawarudo.holo.commands.games.pokemon.PokemonModule;
import dev.zawarudo.holo.commands.games.pokemon.PokemonSpawnManager;
import dev.zawarudo.holo.commands.music.MusicModule;
import dev.zawarudo.holo.core.command.CommandContextFactory;
import dev.zawarudo.holo.core.misc.GuildListener;
import dev.zawarudo.holo.core.misc.MiscListener;
import dev.zawarudo.holo.core.security.BlacklistService;
import dev.zawarudo.holo.database.SQLManager;
import dev.zawarudo.holo.database.dao.*;
import dev.zawarudo.holo.modules.GitHubClient;
import dev.zawarudo.holo.modules.MerriamWebsterClient;
import dev.zawarudo.holo.modules.akinator.AkinatorSessionManager;
import dev.zawarudo.holo.modules.anime.MediaPlatform;
import dev.zawarudo.holo.modules.anime.MediaSearchService;
import dev.zawarudo.holo.modules.anime.anilist.AniListApiClient;
import dev.zawarudo.holo.modules.anime.anilist.AniListProvider;
import dev.zawarudo.holo.modules.anime.jikan.JikanProvider;
import dev.zawarudo.holo.modules.anime.MediaSearchProvider;
import dev.zawarudo.holo.modules.emotes.EmoteManager;
import dev.zawarudo.holo.modules.xkcd.XkcdSyncService;
import dev.zawarudo.holo.utils.ImageResolver;
import dev.zawarudo.holo.utils.UserResolver;
import moe.kyokobot.libdave.NativeDaveFactory;
import moe.kyokobot.libdave.jda.LDJDADaveSessionFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents an instance of the bot.
 */
public class Holo extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Holo.class);

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final BotExecutors executors;

    private JDA jda;
    private final BotConfig botConfig;
    private GuildConfigManager guildConfigManager;
    private CommandManager commandManager;
    private PermissionManager permissionManager;
    private CommandContextFactory ctxFactory;
    private PokemonSpawnManager pokemonSpawnManager;
    private AkinatorSessionManager akinatorSessionManager;
    private SQLManager sqlManager;
    private GitHubClient gitHubClient;
    private MerriamWebsterClient merriamWebsterClient;
    private EmoteManager emoteManager;
    private ImageResolver imageResolver;
    private UserResolver userResolver;

    private ModuleRegistry moduleRegistry;

    private final EventWaiter waiter;

    public Holo(BotConfig botConfig) {
        this.botConfig = botConfig;
        this.executors = new BotExecutors();
        this.waiter = new EventWaiter();
        init();
    }

    private void init() {
        LOGGER.info("Starting bot...");

        // Create a new JDA instance
        JDABuilder builder = JDABuilder.createDefault(getConfig().getBotToken());
        builder.setAudioModuleConfig(new AudioModuleConfig()
            .withDaveSessionFactory(new LDJDADaveSessionFactory(new NativeDaveFactory())));
        builder.enableIntents(EnumSet.allOf(GatewayIntent.class));
        builder.setChunkingFilter(ChunkingFilter.ALL);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.enableCache(CacheFlag.VOICE_STATE);
        builder.addEventListeners(this, waiter);
        builder.setActivity(Activity.watching(botConfig.getDefaultPrefix() + "help"));
        jda = builder.build();
    }

    public void registerEarlyManagers() {
        try {
            this.sqlManager = new SQLManager();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize early managers", e);
        }
    }

    public void registerManagers() {
        // Register DAOs
        GuildConfigDao guildConfigDao = new GuildConfigDao(sqlManager);
        XkcdDao xkcdDao = new XkcdDao(sqlManager);
        EmoteDao emoteDao = new EmoteDao(sqlManager);
        BlacklistedDao blacklistedDao = new BlacklistedDao(sqlManager);
        CountdownDao countdownDao = new CountdownDao(sqlManager);

        // Register services
        XkcdSyncService xkcdSyncService = new XkcdSyncService(xkcdDao, executors.io());
        BlacklistService blacklistService = new BlacklistService(blacklistedDao);

        // Init anime stuff
        AniListApiClient aniListClient = new AniListApiClient();

        List<MediaSearchProvider> providers = List.of(
            new AniListProvider(aniListClient),
            new JikanProvider()
        );
        List<MediaPlatform> order = List.of(MediaPlatform.MAL_JIKAN, MediaPlatform.ANILIST);
        MediaSearchService mediaSearchService = new MediaSearchService(providers, order, true);

        gitHubClient = new GitHubClient(botConfig.getGitHubToken());
        merriamWebsterClient = new MerriamWebsterClient(botConfig.getDictionaryKey(), botConfig.getThesaurusKey());

        emoteManager = new EmoteManager(emoteDao);
        guildConfigManager = new GuildConfigManager(guildConfigDao);
        pokemonSpawnManager = new PokemonSpawnManager(jda);
        akinatorSessionManager = new AkinatorSessionManager();
        permissionManager = new PermissionManager(blacklistService);

        imageResolver = new ImageResolver();
        userResolver = new UserResolver();

        ctxFactory = new CommandContextFactory();

        // Initialize command modules
        moduleRegistry = new ModuleRegistry();
        moduleRegistry.register(new MusicModule(waiter));
        moduleRegistry.register(new PokemonModule(pokemonSpawnManager));

        // Warning: Commands only get initialized here
        commandManager = new CommandManager(
            waiter,
            moduleRegistry,
            gitHubClient,
            merriamWebsterClient,
            guildConfigManager,
            emoteManager,
            akinatorSessionManager,
            xkcdDao,
            xkcdSyncService,
            blacklistService,
            mediaSearchService,
            countdownDao,
            imageResolver,
            userResolver
        );
    }

    public void registerListeners() {
        jda.addEventListener(
            new CommandListener(commandManager, permissionManager, executors.io(), ctxFactory),
            new MiscListener(emoteManager),
            new GuildListener(guildConfigManager, emoteManager)
        );
    }

    public BotExecutors getExecutors() {
        return executors;
    }

    public JDA getJDA() {
        return jda;
    }

    public BotConfig getConfig() {
        return botConfig;
    }

    public GuildConfigManager getGuildConfigManager() {
        return guildConfigManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public PokemonSpawnManager getPokemonSpawnManager() {
        return pokemonSpawnManager;
    }

    public SQLManager getSQLManager() {
        return sqlManager;
    }

    public GitHubClient getGitHubClient() {
        return gitHubClient;
    }

    public MerriamWebsterClient getMerriamWebsterClient() {
        return merriamWebsterClient;
    }

    public EmoteManager getEmoteManager() {
        return emoteManager;
    }

    public ModuleRegistry getModuleRegistry() {
        return moduleRegistry;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        if (!initialized.compareAndSet(false, true)) {
            LOGGER.warn("onReady fired again; skipping re-initialization");
            return;
        }

        registerManagers();
        registerListeners();
        LOGGER.info("{} is ready!", event.getJDA().getSelfUser().getName());
    }

    public void close() {
        try {
            if (jda != null) jda.shutdown();
        } finally {
            executors.close();
        }
    }
}
