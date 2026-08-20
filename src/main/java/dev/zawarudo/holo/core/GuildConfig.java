package dev.zawarudo.holo.core;

import dev.zawarudo.holo.commands.CommandModule;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the configuration of Holo for a specific {@link Guild}.
 */
public class GuildConfig {

    private final long guildId;
    private String prefix;
    private boolean nsfw;

    private final Set<String> disabledModules = new HashSet<>();

    /**
     * Creates a new GuildConfig instance with the default configurations.
     */
    public GuildConfig(long guildId) {
        this.guildId = guildId;
        prefix = Bootstrap.holo.getConfig().getDefaultPrefix();
    }

    /**
     * Returns the guild this bot configuration is for.
     */
    public long getGuildId() {
        return guildId;
    }

    /**
     * Returns the current prefix of the bot for this guild.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Changes the bot prefix for this guild to the specified string.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Reverts the prefix of the bot to the default one.
     */
    public void revertPrefix() {
        prefix = Bootstrap.holo.getConfig().getDefaultPrefix();
    }

    /**
     * Checks whether NSFW commands are allowed in this guild.
     */
    public boolean isNSFWEnabled() {
        return nsfw;
    }

    /**
     * Sets whether NSFW commands are allowed in this guild.
     */
    public void setAllowNSFW(boolean nsfw) {
        this.nsfw = nsfw;
    }

    public Set<String> getDisabledModules() {
        return Collections.unmodifiableSet(disabledModules);
    }

    public void setDisabledModulesCsv(String csv) {
        disabledModules.clear();
        if (csv == null || csv.isBlank()) return;

        for (String part : csv.split(",")) {
            String id = part.trim().toLowerCase(java.util.Locale.ROOT);
            if (!id.isEmpty()) disabledModules.add(id);
        }
    }

    public String getDisabledModulesCsv() {
        if (disabledModules.isEmpty()) return "";
        return disabledModules.stream()
            .sorted()
            .reduce((a, b) -> a + "," + b)
            .orElse("");
    }

    public boolean isModuleEnabled(CommandModule.ModuleId module) {
        return !disabledModules.contains(module.id());
    }

    public void setModuleEnabled(CommandModule.ModuleId module, boolean enabled) {
        if (enabled) disabledModules.remove(module.id());
        else disabledModules.add(module.id());
    }
}
