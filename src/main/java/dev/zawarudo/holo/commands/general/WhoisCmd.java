package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.UserResolver;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@CommandInfo(name = "whois",
    description = "Returns information about a user or bot.",
    usage = "[user]",
    example = "@Holo",
    alias = {"stalk"},
    category = CommandCategory.GENERAL)
public class WhoisCmd extends AbstractCommand implements ExecutableCommand {

    private final UserResolver userResolver;

    public WhoisCmd(UserResolver userResolver) {
        this.userResolver = userResolver;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        if (ctx.argCount() > 1) {
            ctx.reply().errorEmbed("Incorrect usage. Please provide at most one argument.");
            return;
        }

        Optional<User> userOptional = userResolver.resolveUser(ctx);
        if (userOptional.isEmpty()) {
            ctx.reply().errorEmbed("I couldn't find the given user! Please make sure you provided the correct user id or mentioned them!");
            return;
        }
        User user = userOptional.get();

        Optional<Member> memberOptional = userResolver.resolveGuildMember(user, ctx);

        EmbedBuilder builder = setEmbedWithUserDetails(user, memberOptional.isPresent());
        ctx.member().ifPresent(m -> builder.setFooter("Invoked by " + m.getEffectiveName(), ctx.user().getAvatarUrl()));

        if (memberOptional.isEmpty()) {
            ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(5, TimeUnit.MINUTES, null, ignored -> {
            }));
            return;
        }

        Member member = memberOptional.get();
        setEmbedWithMemberDetails(builder, member);
        builder.setColor(member.getColors().getPrimary());
        ctx.channel().sendMessageEmbeds(builder.build()).queue(msg -> msg.delete().queueAfter(5, TimeUnit.MINUTES, null, ignored -> {
        }));
    }

    private EmbedBuilder setEmbedWithUserDetails(User user, boolean hasMember) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("@" + user.getName() + " (" + user.getIdLong() + ")");
        builder.setThumbnail(user.getEffectiveAvatarUrl() + "?size=1024");

        String accountType = user.isBot() ? "Bot" : "User";
        String creationDate = formatDateTime(user.getTimeCreated().toInstant());

        if (!hasMember) {
            addNonApplicableFields(builder);
        }

        builder.addField("Additional Checks", "Account Type: `" + accountType + "`\n" + "Creation Date: `" + creationDate + "`", false);
        return builder;
    }

    private void addNonApplicableFields(EmbedBuilder builder) {
        String naField = "`N/A`";
        builder.addField("Nickname", naField, false)
            .addField("Join Date", naField, false)
            .addField("Highest Role", naField, true)
            .addField("Hoisted Role", naField, true)
            .addField("Roles", naField, false);
    }

    private void setEmbedWithMemberDetails(EmbedBuilder builder, Member member) {
        builder.setDescription("`" + member.getEffectiveName() + "` " + member.getAsMention());
        builder.addField("Join Date", "`" + formatDateTime(member.getTimeJoined().toInstant()) + "`", false);

        List<Role> roles = member.getRoles();
        if (roles.isEmpty()) {
            builder.addField("Highest Role", "@everyone", true)
                .addField("Hoisted Role", "`Unhoisted`", true)
                .addField("Roles", "@everyone", false);
        } else {
            addRoleFields(builder, roles);
        }
    }

    private void addRoleFields(EmbedBuilder builder, List<Role> roles) {
        Role highest = Collections.max(roles, Comparator.comparingInt(Role::getPosition));
        Role hoisted = roles.stream().filter(Role::isHoisted).max(Comparator.comparingInt(Role::getPosition)).orElse(null);

        builder.addField("Highest Role", highest.getAsMention(), true)
            .addField("Hoisted Role", hoisted != null ? hoisted.getAsMention() : "`Unhoisted`", true)
            .addBlankField(true);

        builder.addField("Roles", buildRolesString(roles).toString(), false);
    }

    private StringBuilder buildRolesString(List<Role> roles) {
        StringBuilder rolesString = new StringBuilder();
        int displayedRoles = Math.min(roles.size(), 10);

        for (int i = 0; i < displayedRoles; i++) {
            Role role = roles.get(i);
            if (i > 0) rolesString.append(", ");
            rolesString.append(role.getAsMention());

            if (i == displayedRoles - 1 && roles.size() > 10) {
                rolesString.append(", `and ").append(roles.size() - displayedRoles).append(" more...`");
            }
        }
        return rolesString;
    }

    private String formatDateTime(Instant instant) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("Europe/Zurich"));
        return localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT));
    }
}
