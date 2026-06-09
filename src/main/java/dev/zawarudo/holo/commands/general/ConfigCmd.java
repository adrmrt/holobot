package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.CommandModule;
import dev.zawarudo.holo.commands.ModuleRegistry;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.core.GuildConfig;
import dev.zawarudo.holo.core.GuildConfigManager;
import dev.zawarudo.holo.commands.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

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
public class ConfigCmd extends AbstractCommand {

    private final GuildConfigManager configManager;
    private final ModuleRegistry moduleRegistry;

    public ConfigCmd(GuildConfigManager configManager, ModuleRegistry moduleRegistry) {
        this.configManager = configManager;
        this.moduleRegistry = moduleRegistry;
    }

    @Override
    public void onCommand(@NotNull MessageReceivedEvent event) {
        deleteInvoke(event);

        GuildConfig cfg = configManager.getOrCreate(event.getGuild());

        if (args.length == 0) {
            showCurrentConfig(event, cfg);
            return;
        }

        String config = args[0].toLowerCase(Locale.ROOT);

        switch (config) {
            case "prefix" -> {
                if (args.length == 1) showPrefixInfo(event, cfg);
                else changePrefix(event, cfg);
            }
            case "nsfw" -> {
                if (args.length == 1) showNSFWInfo(event, cfg);
                else changeNSFW(event, cfg);
            }
            case "modules" -> showModules(event, cfg);
            case "module" -> showModule(event, cfg);
            case "reset" -> resetConfiguration(event);
            default -> showUnknownConfigurationEmbed(event);
        }
    }

    private void showCurrentConfig(MessageReceivedEvent event, GuildConfig cfg) {
        String prefix = cfg.getPrefix();

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle(String.format("Bot Configuration for %s", event.getGuild().getName()))
            .setThumbnail(event.getGuild().getIconUrl())
            .setDescription(
                "Here you can see all my configurations for this server.\n" +
                    "To see a specific configuration and how to change it, run:\n" +
                    Formatter.asCodeBlock(prefix + "config <config_name>")
            )
            .addField("Prefix", Formatter.asCodeBlock(prefix), true)
            .addField("NSFW", Formatter.asCodeBlock(cfg.isNSFWEnabled() ? "Enabled" : "Disabled"), true)
            .addField("Modules", Formatter.asCodeBlock(prefix + "config modules"), false);

        sendEmbed(event, builder, true, 5, TimeUnit.MINUTES);
    }

    private void showPrefixInfo(MessageReceivedEvent event, GuildConfig cfg) {
        String prefix = cfg.getPrefix();

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle(String.format("Bot Prefix for %s", event.getGuild().getName()))
            .setDescription(
                "The prefix is needed to run commands.\n" +
                    "To change my prefix, run:\n" +
                    Formatter.asCodeBlock(prefix + "config prefix <new_prefix>")
            )
            .addField("Current Prefix", Formatter.asCodeBlock(prefix), false);

        sendEmbed(event, builder, true, 5, TimeUnit.MINUTES);
    }

    private void changePrefix(MessageReceivedEvent event, GuildConfig cfg) {
        String newPrefix = args[1];
        cfg.setPrefix(newPrefix);

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Prefix Changed")
            .addField("New Prefix", Formatter.asCodeBlock(newPrefix), false);

        sendEmbed(event, builder, true, 1, TimeUnit.MINUTES);
        saveChanges(event, cfg);
    }

    private void showNSFWInfo(MessageReceivedEvent event, GuildConfig cfg) {
        String prefix = cfg.getPrefix();

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle(String.format("NSFW Configuration for %s", event.getGuild().getName()))
            .setDescription(
                "This configuration determines if commands that might be considered NSFW are allowed.\n" +
                    "If enabled, NSFW commands can only be used in channels marked as 18+.\n" +
                    "If disabled, no NSFW commands can be used in this server.\n\n" +
                    "To change it, run:\n" +
                    Formatter.asCodeBlock(prefix + "config nsfw <true/false>")
            )
            .addField("Status", Formatter.asCodeBlock(cfg.isNSFWEnabled() ? "Enabled" : "Disabled"), false);

        sendEmbed(event, builder, true, 5, TimeUnit.MINUTES);
    }

    private void changeNSFW(MessageReceivedEvent event, GuildConfig cfg) {
        boolean nsfw = Boolean.parseBoolean(args[1]);
        cfg.setAllowNSFW(nsfw);

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("NSFW Config Changed")
            .setDescription("NSFW commands are now **" + (nsfw ? "enabled" : "disabled") + "**.");

        sendEmbed(event, builder, true, 1, TimeUnit.MINUTES);
        saveChanges(event, cfg);
    }

    private void showModules(MessageReceivedEvent event, GuildConfig cfg) {
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

        sendEmbed(event, builder, true, 5, TimeUnit.MINUTES);
    }

    private void showModule(MessageReceivedEvent event, GuildConfig cfg) {
        String prefix = cfg.getPrefix();

        if (args.length < 2) {
            EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Missing module id")
                .setDescription("Usage:\n" + Formatter.asCodeBlock(prefix + "config module <moduleId>"));

            sendEmbed(event, builder, true, 1, TimeUnit.MINUTES);
            return;
        }

        String rawId = args[1].toLowerCase(Locale.ROOT);

        CommandModule.ModuleId moduleId = CommandModule.ModuleId.fromId(rawId).orElse(null);
        if (moduleId == null) {
            EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Unknown module")
                .setDescription(
                    "I don't know a module with the id `" + rawId + "`.\n" +
                        "See all modules with:\n" +
                        Formatter.asCodeBlock(prefix + "config modules")
                );

            sendEmbed(event, builder, true, 1, TimeUnit.MINUTES);
            return;
        }

        CommandModule module = moduleRegistry.find(moduleId).orElse(null);
        if (module == null) {
            EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Module not registered")
                .setDescription("Module `" + moduleId.id() + "` is known but not registered.");

            sendEmbed(event, builder, true, 1, TimeUnit.MINUTES);
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

        sendEmbed(event, builder, true, 5, TimeUnit.MINUTES);
    }

    private void resetConfiguration(MessageReceivedEvent event) {
        GuildConfig newCfg = configManager.resetConfigurationForGuild(event.getGuild());

        String prefix = newCfg.getPrefix();

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Reset Bot Configuration")
            .setDescription("My configuration for this server has been reset to the default settings.")
            .addField("Prefix", Formatter.asCodeBlock(prefix), false);

        sendEmbed(event, builder, true, 30, TimeUnit.SECONDS);

        saveChanges(event, newCfg);
    }

    private void showUnknownConfigurationEmbed(MessageReceivedEvent event) {
        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Unknown Configuration")
            .setDescription(String.format("I don't know a configuration with the name `%s`. To see a " +
                "list of configurations, use the following command:```%sconfig```", args[0], getPrefix(event)));
        sendEmbed(event, builder, true, 1, TimeUnit.MINUTES);
    }

    /**
     * Saves the new configuration in the database.
     */
    private void saveChanges(MessageReceivedEvent event, GuildConfig config) {
        try {
            configManager.persist(config);
        } catch (SQLException ex) {
            sendErrorEmbed(event, "Something went wrong while updating your configuration in the " +
                "database. We will try to fix this ASAP.");
            logger.error("Something went wrong while storing the updated config in the database.", ex);
        }
    }
}
