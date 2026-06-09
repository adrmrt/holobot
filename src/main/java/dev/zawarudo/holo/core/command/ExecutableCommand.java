package dev.zawarudo.holo.core.command;

import org.jetbrains.annotations.NotNull;

/**
 * Interface representing the individual commands of the bot.
 */
public interface ExecutableCommand {

    /**
     * Executes the command for the given invocation context.
     *
     * @param ctx the immutable context for this invocation.
     */
    void execute(@NotNull CommandContext ctx);
}
