package dev.zawarudo.holo.modules;

import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import dev.zawarudo.holo.utils.exceptions.NotFoundException;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MerriamWebsterClientIT {

    private static MerriamWebsterClient client;

    @BeforeAll
    static void setup() {
        Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

        String dictionaryKey = dotenv.get("KEY_DICTIONARY");
        String thesaurusKey = dotenv.get("KEY_THESAURUS");

        Assumptions.assumeTrue(
            dictionaryKey != null && !dictionaryKey.isBlank(),
            "KEY_DICTIONARY not set – skipping Merriam-Webster tests"
        );
        Assumptions.assumeTrue(
            thesaurusKey != null && !thesaurusKey.isBlank(),
            "KEY_THESAURUS not set – skipping Merriam-Webster tests"
        );

        client = new MerriamWebsterClient(dictionaryKey, thesaurusKey);
    }

    @Test
    @Order(1)
    void dictionaryLookup_returnsEntriesForKnownWord() throws APIException, InvalidRequestException, NotFoundException {
        var result = client.lookupDictionary("syzygy");

        assertNotNull(result);
        assertTrue(result.hasEntries(), "Expected dictionary entries for a known word");
        assertFalse(result.hasSuggestions(), "Expected no suggestions when entries exist");

        assertNotNull(result.entries());
        assertNotNull(result.suggestions());

        var first = result.entries().getFirst();
        assertNotNull(first);

        assertTrue(first.shortDefs() != null && !first.shortDefs().isEmpty(),
            "Expected at least one short definition");

        assertNotNull(first.shortDefs());
        assertNotNull(first.synonyms());
        assertNotNull(first.antonyms());
        assertNotNull(first.examples());

        assertTrue(first.shortDefs().stream().anyMatch(d -> d != null && !d.isBlank()),
            "Expected non-blank definition text");
    }

    @Test
    @Order(2)
    void dictionaryLookup_returnsSuggestionsForMisspelling() throws APIException, InvalidRequestException, NotFoundException {
        var result = client.lookupDictionary("syzygii");

        assertNotNull(result);

        assertTrue(result.hasSuggestions() || result.hasEntries(),
            "Expected suggestions (or entries if MW treats it as valid)");

        if (result.hasSuggestions() && !result.hasEntries()) {
            assertFalse(result.suggestions().isEmpty());
            assertTrue(result.suggestions().stream().allMatch(s -> s != null && !s.isBlank()),
                "Suggestions should be non-blank strings");
        }
    }

    @Test
    @Order(3)
    void dictionaryLookup_blankInput_returnsEmptyResult() throws APIException, InvalidRequestException, NotFoundException {
        var result = client.lookupDictionary("   ");

        assertNotNull(result);
        assertFalse(result.hasEntries());
        assertFalse(result.hasSuggestions());
        assertTrue(result.entries().isEmpty());
        assertTrue(result.suggestions().isEmpty());
    }

    @Test
    @Order(4)
    void dictionaryLookup_examples_areCleanIfPresent() throws APIException, InvalidRequestException, NotFoundException {
        var result = client.lookupDictionary("run");

        assertNotNull(result);

        if (!result.hasEntries()) {
            Assumptions.assumeTrue(false, "No dictionary entries returned for 'run'; skipping examples assertion.");
        }

        List<String> allExamples = result.entries().stream()
            .flatMap(e -> e.examples().stream())
            .toList();

        if (allExamples.isEmpty()) {
            Assumptions.assumeTrue(false, "No examples returned for 'run'; skipping examples assertion.");
        }

        assertTrue(allExamples.stream().allMatch(ex -> ex != null && !ex.isBlank()),
            "Examples should be non-blank");
        assertTrue(allExamples.stream().noneMatch(ex -> ex.contains("{") || ex.contains("}")),
            "Examples should not contain MW formatting tokens after cleanup");
    }

    @Test
    @Order(5)
    void thesaurusLookup_returnsEntriesForKnownWord() throws APIException, InvalidRequestException, NotFoundException {
        var result = client.lookupThesaurus("happy");

        assertNotNull(result);
        assertTrue(result.hasEntries(), "Expected thesaurus entries for a known word");
        assertFalse(result.hasSuggestions(), "Expected no suggestions when entries exist");

        var first = result.entries().getFirst();

        // Lists should never be null
        assertNotNull(first.synonyms());
        assertNotNull(first.antonyms());
        assertNotNull(first.shortDefs());
        assertNotNull(first.examples());

        assertTrue(
            !first.synonyms().isEmpty() || !first.antonyms().isEmpty(),
            "Expected at least synonyms or antonyms for thesaurus entries"
        );
    }

    @Test
    @Order(6)
    void thesaurusLookup_returnsSuggestionsForMisspelling() throws APIException, InvalidRequestException, NotFoundException {
        var result = client.lookupThesaurus("hapy");

        assertNotNull(result);
        assertTrue(result.hasSuggestions() || result.hasEntries(),
            "Expected suggestions (or entries if MW treats it as valid)");

        if (result.hasSuggestions() && !result.hasEntries()) {
            assertTrue(result.suggestions().stream().allMatch(s -> s != null && !s.isBlank()),
                "Suggestions should be non-blank strings");
        }
    }

    @Test
    @Order(7)
    void invalidKeys_throwClientError() {
        MerriamWebsterClient bad = new MerriamWebsterClient("invalid", "invalid");

        assertThrows(
            InvalidRequestException.class,
            () -> bad.lookupDictionary("test"),
            "Expected InvalidRequestException for invalid dictionary key"
        );

        assertThrows(
            InvalidRequestException.class,
            () -> bad.lookupThesaurus("test"),
            "Expected InvalidRequestException for invalid thesaurus key"
        );
    }
}
