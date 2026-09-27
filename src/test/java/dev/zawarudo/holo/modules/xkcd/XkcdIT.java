package dev.zawarudo.holo.modules.xkcd;

import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XkcdIT {

    private static XkcdComic latest;
    private static XkcdComic comic;

    @BeforeAll
    static void setup() throws APIException, InvalidRequestException {
        latest = XkcdAPI.getLatest();
        comic = XkcdAPI.getComic(1234);
    }

    @Test
    void testAssertTitleNotNull1() {
        assertNotNull(latest.getTitle(), "Title should not be null");
    }

    @Test
    void testAssertTitleNotNull2() {
        assertNotNull(comic.getTitle(), "Title should not be null");
    }

    @Test
    void testAssertCorrectIssueNr1() {
        assertNotEquals(0, latest.getIssueNr(), "Issue number should not be 0");
    }

    @Test
    void testAssertCorrectIssueNr2() {
        assertNotEquals(0, comic.getIssueNr(), "Issue number should not be 0");
    }

    @Test
    void testAssertCorrectDate() {
        assertEquals("5/7/2013", comic.getDate(), "Date should be equal");
    }

    @Test
    void testAssertThrowsInvalidRequest1() {
        assertThrows(InvalidRequestException.class, () -> XkcdAPI.getComic(-1), "Method should throw an exception");
    }

    @Test
    void testAssertThrowsInvalidRequest2() {
        assertThrows(InvalidRequestException.class, () -> XkcdAPI.getComic(10000), "Method should throw an exception");
    }
}