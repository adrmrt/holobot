package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.utils.VersionInfo;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.database.Database;
import dev.zawarudo.holo.utils.Formatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "info",
		description = "Shows information about me",
		alias = {"source", "bot", "sauce"},
		category = CommandCategory.GENERAL)
public class BotInfoCmd extends AbstractCommand {

	@Override
	public void onCommand(@NotNull MessageReceivedEvent e) {
		deleteInvoke(e);
		
		String systemInfo = "**CPU:** " + cpuInfo() + "\n"
						+ "**Memory:** " + memoryInfo() + "\n"
						+ "**Uptime:** `" + Formatter.formatTime(System.currentTimeMillis() - Bootstrap.getStartupTime()) + "`";

		String description = "Use `" + getPrefix(e) + "help` to see all commands";

		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle(e.getJDA().getSelfUser().getName() + " | Information");
		builder.setThumbnail(e.getJDA().getSelfUser().getEffectiveAvatarUrl().concat("?size=512"));
		builder.setDescription(description);
		builder.addField("Creator", "<@" + Bootstrap.holo.getConfig().getOwnerId() + ">", false);
		builder.addField("Bot Version", "`" + VersionInfo.getBotVersion() + "`", false);
		builder.addField("JDA Version", "`" + JDAInfo.VERSION.replace("_" + JDAInfo.COMMIT_HASH, "") + "`", false);
		builder.addField("Java Version", "`" + VersionInfo.getJavaVersion() + "`", false);
		builder.addField("System Information", systemInfo, false);
		builder.addField("Database Size", "`" + new File(Database.getDbPath()).length() / 1024 / 1024 + "MB`", false);
		builder.addField("Source", "[GitHub](https://github.com/xHarlock/HoloBot)", false);

		sendEmbed(e, builder, true, 1, TimeUnit.MINUTES);
	}

	private static String cpuInfo() {
		OperatingSystemMXBean base = ManagementFactory.getOperatingSystemMXBean();
		int cores = base.getAvailableProcessors();

		if (base instanceof com.sun.management.OperatingSystemMXBean os) {
			double system = os.getCpuLoad();
			double proc   = os.getProcessCpuLoad();

			String systemStr = system >= 0 ? String.format("%.1f%%", system * 100) : "N/A";
			String procStr   = proc >= 0 ? String.format("%.1f%%", proc * 100) : "N/A";

			return "`System: " + systemStr + " | Bot: " + procStr + " | " + cores + " core(s)`";
		}

		// Fallback
		double load = base.getSystemLoadAverage();
		if (load < 0) {
			return "`N/A | " + cores + " core(s)`";
		}
		double approx = (load / cores) * 100.0;
		return "`~" + String.format("%.1f%%", approx) + " load | " + cores + " core(s)`";
	}

	private static String memoryInfo() {
		MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		MemoryUsage heap = memory.getHeapMemoryUsage();

		long used = heap.getUsed();
		long max = heap.getMax();

		String usedMb = formatMb(used);

		if (max > 0) {
			String maxMb = formatMb(max);
			double pct = (used / (double) max) * 100.0;

			return "`" + usedMb + " / " + maxMb
					+ " (" + String.format("%.1f%%", pct) + ")`";
		}

		// Fallback if max heap is undefined
		return "`" + usedMb + "`";
	}

	private static String formatMb(long bytes) {
		return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
	}
}