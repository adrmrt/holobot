package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.commands.CommandModule;
import dev.zawarudo.holo.commands.ModuleRegistry;
import dev.zawarudo.holo.core.GuildConfig;
import dev.zawarudo.holo.core.GuildConfigManager;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Command to change the configurations of the bot for the guild.
 */
@CommandInfo(name = "config",
    description = "See and change the configuration of the bot for this guild.",
    ownerOnly = true,
    category = CommandCategory.GENERAL)
public class ConfigCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigCmd.class);

    private final GuildConfigManager configManager;
    private final ModuleRegistry moduleRegistry;

    public ConfigCmd(GuildConfigManager configManager, ModuleRegistry moduleRegistry) {
        this.configManager = configManager;
        this.moduleRegistry = moduleRegistry;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        GuildConfig cfg = configManager.getOrCreate(ctx.guild().orElseThrow());

        if (!ctx.hasArgs()) {
            showCurrentConfig(ctx, cfg);
            return;
        }

        String config = ctx.args().getFirst().toLowerCase(Locale.ROOT);

        switch (config) {
            case "prefix" -> {
                if (ctx.argCount() == 1) showPrefixInfo(ctx, cfg);
                else changePrefix(ctx, cfg);
            }
            case "nsfw" -> {
                if (ctx.argCount() == 1) showNSFWInfo(ctx, cfg);
                else changeNSFW(ctx, cfg);
            }
            case "modules" -> showModules(ctx, cfg);
            case "module" -> showModule(ctx, cfg);
            case "reset" -> resetConfiguration(ctx);
            default -> showUnknownConfigurationEmbed(ctx, cfg);
        }
    }

    private void showCurrentConfig(CommandContext ctx, GuildConfig cfg) {
        String prefix = cfg.getPrefix();
        Guild guild = ctx.guild().orElseThrow();

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle(String.format("Bot Configuration for %s", guild.getName()))
            .setThumbnail(guild.getIconUrl())
            .setDescription(
                "Here you can see all my configurations for this server.\n" +
                    "To see a specific configuration and how to change it, run:\n" +
                    Formatter.asCodeBlock(prefix + "config <config_name>")
            )
            .addField("Prefix", Formatter.asCodeBlock(prefix), true)
            .addField("NSFW", Formatter.asCodeBlock(cfg.isNSFWEnabled() ? "Enabled" : "Disabled"), true)
            .addField("Modules", Formatter.asCodeBlock(prefix + "config modules"), false);

        addFooter(ctx, builder);
        ctx.reply().embed(builder.build(), 5, TimeUnit.MINUTES);
    }

    private void showPrefixInfo(CommandContext ctx, GuildConfig cfg) {
        String prefix = cfg.getPrefix();
        Guild guild = ctx.guild().orElseThrow();

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle(String.format("Bot Prefix for %s", guild.getName()))
            .setDescription(
                "The prefix is needed to run commands.\n" +
                    "To change my prefix, run:\n" +
                    Formatter.asCodeBlock(prefix + "config prefix <new_prefix>")
            )
            .addField("Current Prefix", Formatter.asCodeBlock(prefix), false);

        addFooter(ctx, builder);
        ctx.reply().embed(builder.build(), 5, TimeUnit.MINUTES);
    }

    private void changePrefix(CommandContext ctx, GuildConfig cfg) {
        String newPrefix = ctx.args().get(1);
        cfg.setPrefix(newPrefix);

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Prefix Changed")
            .addField("New Prefix", Formatter.asCodeBlock(newPrefix), false);

        addFooter(ctx, builder);
        ctx.reply().embed(builder.build(), 1, TimeUnit.MINUTES);
        saveChanges(ctx, cfg);
    }

    private void showNSFWInfo(CommandContext ctx, GuildConfig cfg) {
        String prefix = cfg.getPrefix();
        Guild guild = ctx.guild().orElseThrow();

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle(String.format("NSFW Configuration for %s", guild.getName()))
            .setDescription(
                "This configuration determines if commands that might be considered NSFW are allowed.\n" +
                    "If enabled, NSFW commands can only be used in channels marked as 18+.\n" +
                    "If disabled, no NSFW commands can be used in this server.\n\n" +
                    "To change it, run:\n" +
                    Formatter.asCodeBlock(prefix + "config nsfw <true/false>")
            )
            .addField("Status", Formatter.asCodeBlock(cfg.isNSFWEnabled() ? "Enabled" : "Disabled"), false);

        addFooter(ctx, builder);
        ctx.reply().embed(builder.build(), 5, TimeUnit.MINUTES);
    }

    private void changeNSFW(CommandContext ctx, GuildConfig cfg) {
        boolean nsfw = Boolean.parseBoolean(ctx.args().get(1));
        cfg.setAllowNSFW(nsfw);

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("NSFW Config Changed")
            .setDescription("NSFW commands are now **" + (nsfw ? "enabled" : "disabled") + "**.");

        addFooter(ctx, builder);
        ctx.reply().embed(builder.build(), 1, TimeUnit.MINUTES);
        saveChanges(ctx, cfg);
    }

    private void showModules(CommandContext ctx, GuildConfig cfg) {
        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Command Modules")
            .setDescription(
                "List of command modules and their status.\n" +
                    "For details about a module, use:\n" +
                    Formatter.asCodeBlock(cfg.getPrefix() + "config module <moduleId>")
            );

        boolean any = false;

        for (CommandModule module : moduleRegistry.all()) {
            any = true;
            boolean enabled = cfg.isModuleEnabled(module.id());
            builder.addField(module.id().id(), Formatter.asCodeBlock(enabled ? "Enabled" : "Disabled"), false);
        }

        if (!any) {
            builder.addField("No modules", "No modules registered.", false);
        }

        addFooter(ctx, builder);
        ctx.reply().embed(builder.build(), 5, TimeUnit.MINUTES);
    }

    private void showModule(CommandContext ctx, GuildConfig cfg) {
        String prefix = cfg.getPrefix();

        if (ctx.argCount() < 2) {
            EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Missing module id")
                .setDescription("Usage:\n" + Formatter.asCodeBlock(prefix + "config module <moduleId>"));

            addFooter(ctx, builder);
            ctx.reply().embed(builder.build(), 1, TimeUnit.MINUTES);
            return;
        }

        String rawId = ctx.args().get(1).toLowerCase(Locale.ROOT);

        CommandModule.ModuleId moduleId = CommandModule.ModuleId.fromId(rawId).orElse(null);
        if (moduleId == null) {
            EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Unknown module")
                .setDescription(
                    "I don't know a module with the id `" + rawId + "`.\n" +
                        "See all modules with:\n" +
                        Formatter.asCodeBlock(prefix + "config modules")
                );

            addFooter(ctx, builder);
            ctx.reply().embed(builder.build(), 1, TimeUnit.MINUTES);
            return;
        }

        CommandModule module = moduleRegistry.find(moduleId).orElse(null);
        if (module == null) {
            EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Module not registered")
                .setDescription("Module `" + moduleId.id() + "` is known but not registered.");

            addFooter(ctx, builder);
            ctx.reply().embed(builder.build(), 1, TimeUnit.MINUTES);
            return;
        }

        boolean enabled = cfg.isModuleEnabled(module.id());

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Module: " + module.id().id())
            .setDescription(module.description())
            .addField("Status", Formatter.asCodeBlock(enabled ? "Enabled" : "Disabled"), false)
            .addField(
                "Hint",
                "Change status via:\n" + Formatter.asCodeBlock(prefix + "config modules <enable|disable> " + module.id().id()),
                false
            );

        addFooter(ctx, builder);
        ctx.reply().embed(builder.build(), 5, TimeUnit.MINUTES);
    }

    private void resetConfiguration(CommandContext ctx) {
        GuildConfig newCfg = configManager.resetConfigurationForGuild(ctx.guild().orElseThrow());

        String prefix = newCfg.getPrefix();

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Reset Bot Configuration")
            .setDescription("My configuration for this server has been reset to the default settings.")
            .addField("Prefix", Formatter.asCodeBlock(prefix), false);

        addFooter(ctx, builder);
        ctx.reply().embed(builder.build(), 30, TimeUnit.SECONDS);

        saveChanges(ctx, newCfg);
    }

    private void showUnknownConfigurationEmbed(CommandContext ctx, GuildConfig cfg) {
        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Unknown Configuration")
            .setDescription(String.format("I don't know a configuration with the name `%s`. To see a " +
                "list of configurations, use the following command:```%sconfig```", ctx.args().getFirst(), cfg.getPrefix()));

        addFooter(ctx, builder);
        ctx.reply().embed(builder.build(), 1, TimeUnit.MINUTES);
    }

    /**
     * Adds the standard "Invoked by" footer to the embed if the command was invoked by a guild member.
     */
    private void addFooter(CommandContext ctx, EmbedBuilder builder) {
        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getEffectiveAvatarUrl()));
    }

    /**
     * Saves the new configuration in the database.
     */
    private void saveChanges(CommandContext ctx, GuildConfig config) {
        try {
            configManager.persist(config);
        } catch (SQLException ex) {
            ctx.reply().errorEmbed("Something went wrong while updating your configuration in the " +
                "database. We will try to fix this ASAP.");
            LOGGER.error("Something went wrong while storing the updated config in the database.", ex);
        }
    }
}
