package dev.zawarudo.holo.commands.fun;

import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Random;

@CommandInfo(name = "coinflip",
    description = "Flips a coin. You can provide an additional argument as the number of times I should flip a coin with the limit being 1'000'000 coin flips at once.",
    usage = "[<times>]",
    category = CommandCategory.MISC)
public class CoinFlipCmd extends AbstractCommand implements ExecutableCommand {

    private static final int MAX_COIN_FLIPS = 1_000_000;
    private static final Random RANDOM = new Random();

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.reply().typing();

        int times = parseTimes(ctx);
        if (times == -1) {
            ctx.reply().text("Invalid argument! Please provide a positive non-zero integer as argument.");
            return;
        }
        if (times > MAX_COIN_FLIPS) {
            ctx.reply().text(String.format("Hold on! My limit is %d coin flips at once.", MAX_COIN_FLIPS));
            return;
        }

        int heads = flipCoins(times);
        int tails = times - heads;

        ctx.reply().text(formatMessage(times, heads, tails));
    }

    private int flipCoins(int times) {
        int heads = 0;
        for (int i = 0; i < times; i++) {
            int result = RANDOM.nextInt(2);
            if (result == 0) {
                heads++;
            }
        }
        return heads;
    }

    private String formatMessage(int times, int heads, int tails) {
        String text = "I flipped a coin **%s** times. I got **%s** heads and **%s** tails.";
        DecimalFormat df = new DecimalFormat("###,###,###");
        return String.format(text, df.format(times), df.format(heads), df.format(tails)).replace(",", "'");
    }

    private int parseTimes(CommandContext ctx) {
        if (ctx.args().isEmpty()) return 1;

        String raw = ctx.args().getFirst();
        try {
            int n = Integer.parseInt(raw);
            return (n >= 1) ? n : -1;
        } catch (NumberFormatException _) {
            return -1;
        }
    }
}
