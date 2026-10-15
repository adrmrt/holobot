package dev.zawarudo.holo.commands.games.pokemon;

import dev.zawarudo.holo.commands.games.pokemon.PokeLbCmd.ResetArgs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PokeLbCmdTest {

    @Test
    void wholeServer() {
        assertEquals(Optional.of(new ResetArgs(null, false)), PokeLbCmd.parseResetArgs(List.of("reset")));
        assertEquals(Optional.of(new ResetArgs(null, true)), PokeLbCmd.parseResetArgs(List.of("reset", "CONFIRM")));
    }

    @Test
    void singleUserByMentionOrId() {
        assertEquals(Optional.of(new ResetArgs(123456789012345678L, false)),
            PokeLbCmd.parseResetArgs(List.of("reset", "<@123456789012345678>")));
        assertEquals(Optional.of(new ResetArgs(123456789012345678L, true)),
            PokeLbCmd.parseResetArgs(List.of("reset", "123456789012345678", "confirm")));
    }

    @Test
    void malformedArgs() {
        assertEquals(Optional.empty(), PokeLbCmd.parseResetArgs(List.of("reset", "everyone")));
        assertEquals(Optional.empty(), PokeLbCmd.parseResetArgs(List.of("reset", "<@1>", "<@2>")));
        assertEquals(Optional.empty(), PokeLbCmd.parseResetArgs(List.of("reset", "confirm", "<@1>")));
        assertEquals(Optional.empty(), PokeLbCmd.parseResetArgs(List.of("reset", "12345678901234567890")));
    }
}
