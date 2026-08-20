package dev.zawarudo.holo.commands.owner;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.annotations.Deactivated;
import org.jetbrains.annotations.NotNull;

@Deactivated
@CommandInfo(name = "nickname",
    description = "Changes the nickname of the bot or of a specified user.",
    usage = "<user> <nickname>",
    alias = {"nick"},
    ownerOnly = true,
    category = CommandCategory.OWNER)
public class NicknameCmd implements CommandMetadata, ExecutableCommand {

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        // TODO: Check for other cases, such as when no argument was given

        if (ctx.hasArgs() && ctx.args().getFirst().equals("self")) {
            String nick = String.join(" ", ctx.args().subList(1, ctx.args().size()));
            ctx.guild().orElseThrow().getMember(ctx.jda().getSelfUser()).modifyNickname(nick).queue();
        } else if (ctx.hasArgs()) {
            long id = Long.parseLong(ctx.args().getFirst());
            String nick = String.join(" ", ctx.args().subList(1, ctx.args().size()));
            ctx.guild().orElseThrow().getMemberById(id).modifyNickname(nick).queue();
        }
    }
}
