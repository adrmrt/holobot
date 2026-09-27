package dev.zawarudo.holo.modules.urbandictionary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UrbanDictionaryScraperIT {

    private final UrbanDictionaryScraper scraper = new UrbanDictionaryScraper();

    // A redesign makes the selectors match nothing, which returns an empty list instead of throwing
    @Test
    @Timeout(15)
    void fetch_commonTerm_parsesEntries() throws Exception {
        List<UrbanDictionaryEntry> entries = scraper.fetch("yeet");

        assertFalse(entries.isEmpty(), "No entries parsed - div.definition selector may be outdated");

        UrbanDictionaryEntry first = entries.getFirst();
        assertFalse(first.term() == null || first.term().isBlank(), "Title selector may be outdated");
        assertTrue(first.hasValidDefinition(), "Meaning selector may be outdated");
        assertTrue(first.link() != null && first.link().startsWith("https://"), "Link extraction may be outdated");
    }

    @Test
    @Timeout(15)
    void fetch_nonsenseTerm_returnsEmpty() throws Exception {
        assertTrue(scraper.fetch("qzxvjkwpqzxvjkwp").isEmpty());
    }
}
