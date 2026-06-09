package dev.zawarudo.holo.commands.games;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.modules.akinator.AkinatorSession;
import dev.zawarudo.holo.modules.akinator.AkinatorSessionManager;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.entities.ISnowflake;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

@CommandInfo(
    name = "akinator",
    description = "Play Akinator using buttons.",
    category = CommandCategory.GAMES,
    embedColor = EmbedColor.AKINATOR
)
public class AkinatorCmd extends AbstractCommand implements ExecutableCommand {

    private final EventWaiter waiter;
    private final AkinatorSessionManager sessions;

    public AkinatorCmd(@NotNull EventWaiter waiter, @NotNull AkinatorSessionManager sessions) {
        this.waiter = waiter;
        this.sessions = sessions;
    }

    @Override
    public void execute(@NonNull CommandContext ctx) {
        long userId = ctx.user().getIdLong();

        if (sessions.hasActiveSession(userId)) {
            ctx.reply().text("You already have an active Akinator game running. Please finish that game before starting a new one.");
            return;
        }

        ctx.reply().typing();

        long guildId = ctx.guild()
            .map(ISnowflake::getIdLong)
            .orElse(0L);

        AkinatorSession session = new AkinatorSession(
            userId,
            guildId,
            guildId,
            waiter,
            sessions
        );

        if (!sessions.registerSession(session)) {
            ctx.reply().text("You already have an active Akinator game running. Please finish that game before starting a new one.");
            return;
        }

        ctx.message().ifPresentOrElse(
            session::start,
            () -> ctx.reply().text("This command currently requires a message-based invocation.")
        );
    }
}
