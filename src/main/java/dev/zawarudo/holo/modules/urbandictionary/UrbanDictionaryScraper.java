package dev.zawarudo.holo.modules.urbandictionary;

import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.HoloHttp;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class UrbanDictionaryScraper {

    private static final String BASE_URL = "https://www.urbandictionary.com";
    private static final int MAX_RESULTS = 10;

    public @NotNull List<UrbanDictionaryEntry> fetch(@NotNull String searchTerm) throws IOException {
        String url = String.format("%s/define.php?term=%s", BASE_URL, Formatter.encodeUrl(searchTerm));

        Document doc;
        try {
            doc = Jsoup.connect(url)
                    .userAgent(HoloHttp.DEFAULT_USER_AGENT)
                    .timeout(10_000)
                    .get();
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 404) {
                return List.of();
            }
            throw e;
        }

        Elements elements = doc.select("div.definition");
        int limit = Math.min(MAX_RESULTS, elements.size());

        List<UrbanDictionaryEntry> entries = new ArrayList<>(limit);

        for (int i = 0; i < limit; i++) {
            Element element = elements.get(i);

            String title = extractTitle(element);
            String meaning = extractMeaning(element);
            String example = extractExample(element);
            String link = extractLink(element);

            entries.add(new UrbanDictionaryEntry(title, meaning, example, link));
        }

        return entries;
    }

    private String extractTitle(Element element) {
        Element el = element.selectFirst("a.word");
        if (el != null) return el.wholeText();
        Element h2 = element.selectFirst("h2");
        return h2 != null ? h2.wholeText() : null;
    }

    private String extractMeaning(Element element) {
        Element meaningElement = element.selectFirst("div.meaning");
        if (meaningElement != null) {
            return toDiscordMarkdown(meaningElement);
        }
        return null;
    }

    private String extractExample(Element element) {
        Element exampleElement = element.selectFirst("div.example");
        if (exampleElement != null) {
            return toDiscordMarkdown(exampleElement);
        }
        return null;
    }

    private String extractLink(Element element) {
        Element el = element.selectFirst("a.word");
        if (el != null) return el.absUrl("href").replace(" ", "%20");

        // Extract defid from share link (e.g. /ui/share?term=troll&defid=5096)
        Element shareLink = element.selectFirst("a[href*='defid=']");
        if (shareLink != null) {
            String href = shareLink.attr("href");
            String defid = extractQueryParam(href, "defid");
            String term = extractQueryParam(href, "term");
            if (defid != null && term != null) {
                String decoded = URLDecoder.decode(term, StandardCharsets.UTF_8);
                return "https://" + decoded.toLowerCase().replace(" ", "-") + ".urbanup.com/" + defid;
            }
        }

        Element h2 = element.selectFirst("h2");
        if (h2 != null) return BASE_URL + "/define.php?term=" + Formatter.encodeUrl(h2.wholeText().trim());
        return null;
    }

    private static String extractQueryParam(String url, String param) {
        String prefix = param + "=";
        int start = url.indexOf(prefix);
        if (start == -1) return null;
        start += prefix.length();
        int end = url.indexOf('&', start);
        return end == -1 ? url.substring(start) : url.substring(start, end);
    }

    /**
     * Converts links inside the HTML element into Markdown links, then returns plain text.
     * This makes links displayable inside Discord embeds.
     */
    private String toDiscordMarkdown(@NotNull Element element) {
        for (Element a : element.select("a[href]")) {
            String href = a.absUrl("href").replace(" ", "%20");
            String linkText = a.text();
            a.replaceWith(new TextNode("[" + linkText + "](" + href + ")"));
        }
        return element.wholeText();
    }
}