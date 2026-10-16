package dev.zawarudo.holo.commands.games.pokemon;

import dev.zawarudo.holo.commands.games.pokemon.PokemonLeaderboardCmd.ResetArgs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PokemonLeaderboardCmdTest {

    @Test
    void wholeServer() {
        assertEquals(Optional.of(new ResetArgs(null, false)), PokemonLeaderboardCmd.parseResetArgs(List.of("reset")));
        assertEquals(Optional.of(new ResetArgs(null, true)), PokemonLeaderboardCmd.parseResetArgs(List.of("reset", "CONFIRM")));
    }

    @Test
    void singleUserByMentionOrId() {
        assertEquals(Optional.of(new ResetArgs(123456789012345678L, false)),
            PokemonLeaderboardCmd.parseResetArgs(List.of("reset", "<@123456789012345678>")));
        assertEquals(Optional.of(new ResetArgs(123456789012345678L, true)),
            PokemonLeaderboardCmd.parseResetArgs(List.of("reset", "123456789012345678", "confirm")));
    }

    @Test
    void malformedArgs() {
        assertEquals(Optional.empty(), PokemonLeaderboardCmd.parseResetArgs(List.of("reset", "everyone")));
        assertEquals(Optional.empty(), PokemonLeaderboardCmd.parseResetArgs(List.of("reset", "<@1>", "<@2>")));
        assertEquals(Optional.empty(), PokemonLeaderboardCmd.parseResetArgs(List.of("reset", "confirm", "<@1>")));
        assertEquals(Optional.empty(), PokemonLeaderboardCmd.parseResetArgs(List.of("reset", "12345678901234567890")));
    }
}
