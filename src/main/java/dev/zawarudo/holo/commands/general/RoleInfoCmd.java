package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CommandInfo(name = "roleinfo",
    description = "Shows information about a role.",
    usage = "<role name, id or mention>",
    adminOnly = true,
    category = CommandCategory.GENERAL)
public class RoleInfoCmd extends AbstractCommand implements ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        if (ctx.args().isEmpty()) {
            ctx.reply().errorEmbed("You must provide a role name or id. Alternatively, you can also ping a role.");
            return;
        }

        Optional<Role> roleOpt = resolveRole(ctx);
        if (roleOpt.isEmpty()) {
            ctx.reply().errorEmbed("Could not find a role with that name or id.");
            return;
        }

        Role role = roleOpt.get();

        Color roleColor = role.getColors().getPrimary();
        EmbedBuilder b = buildRoleInfoEmbed(ctx, role);

        b.setColor(roleColor);

        ctx.reply().embed(b.build(), 5, TimeUnit.MINUTES);
    }

    private Optional<Role> resolveRole(@NotNull CommandContext ctx) {
        Guild guild = ctx.guild().orElseThrow();

        // Mentioned role
        Optional<Role> mentioned = ctx.invocation().mentionedRoles().stream().findFirst();
        if (mentioned.isPresent()) {
            return mentioned;
        }

        // Role by id
        List<String> args = ctx.args();
        if (!args.isEmpty()) {
            String first = args.getFirst();
            Optional<Long> idOpt = parseUnsignedLong(first);
            if (idOpt.isPresent()) {
                Role byId = guild.getRoleById(idOpt.get());
                if (byId != null) return Optional.of(byId);
            }
        }

        // Role by exact name (case-insensitive)
        String wanted = String.join(" ", args).trim().toLowerCase(Locale.ROOT);
        if (wanted.isBlank()) return Optional.empty();

        return guild.getRoles().stream()
            .filter(r -> r.getName().toLowerCase(Locale.ROOT).equals(wanted))
            .findFirst();
    }

    private EmbedBuilder buildRoleInfoEmbed(CommandContext ctx, Role role) {
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle("Role Information");

        if (role.getIcon() != null) {
            b.setThumbnail(role.getIcon().getIconUrl());
        }

        b.setDescription(role.getAsMention() + " (" + role.getId() + ")");

        // Creation date
        LocalDateTime localDateTime = LocalDateTime.ofInstant(
            role.getTimeCreated().toInstant(),
            ZoneId.of("Europe/Zurich")
        );

        String created = localDateTime.format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
        );
        b.addField("Creation Date", "`" + created + "`", false);

        // Member count
        long memberCount = ctx.guild().orElseThrow().getMembers().stream()
            .filter(m -> m.getRoles().contains(role))
            .count();
        b.addField("Members", "`" + memberCount + "`", false);

        // Other info
        String colorHex = role.getColors().getPrimary() == null
            ? "None"
            : String.format("#%02x%02x%02x",
            role.getColors().getPrimary().getRed(),
            role.getColors().getPrimary().getGreen(),
            role.getColors().getPrimary().getBlue()
        );
        b.addField("Color", "`" + colorHex + "`", true);
        b.addField("Hoisted", String.valueOf(role.isHoisted()), true);
        b.addField("Position", String.valueOf(role.getPosition()), true);

        // Permissions
        String perms = role.getPermissions().isEmpty()
            ? "None"
            : role.getPermissions().stream()
              .map(Permission::getName)
              .collect(Collectors.joining("\n"));

        b.addField("Permissions", Formatter.asCodeBlock(perms), false);

        return b;
    }

    private Optional<Long> parseUnsignedLong(String raw) {
        if (raw == null) return Optional.empty();

        String digits = raw.replaceAll("\\D+", "");
        if (digits.isBlank()) return Optional.empty();

        try {
            long v = Long.parseLong(digits);
            return (v > 0) ? Optional.of(v) : Optional.empty();
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
