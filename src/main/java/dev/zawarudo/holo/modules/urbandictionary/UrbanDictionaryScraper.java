package dev.zawarudo.holo.modules.urbandictionary;

import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.HoloHttp;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class UrbanDictionaryScraper {

    private static final String BASE_URL = "https://www.urbandictionary.com";
    private static final int MAX_RESULTS = 10;

    public @NotNull List<UrbanDictionaryEntry> fetch(@NotNull String searchTerm) throws IOException {
        String url = String.format("%s/define.php?term=%s", BASE_URL, Formatter.encodeUrl(searchTerm));

        Document doc = Jsoup.connect(url)
                .userAgent(HoloHttp.DEFAULT_USER_AGENT)
                .timeout(10_000)
                .get();

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
        Element titleElement = element.selectFirst("a.word");
        return (titleElement != null) ? titleElement.wholeText() : null;
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
        Element titleElement = element.selectFirst("a.word");
        return (titleElement != null) ? BASE_URL + titleElement.attr("href").replace(" ", "%20") : null;
    }

    /**
     * Converts links inside the HTML element into Markdown links, then returns plain text.
     * This makes links displayable inside Discord embeds.
     */
    private String toDiscordMarkdown(@NotNull Element element) {
        for (Element a : element.select("a[href]")) {
            String href = (BASE_URL + a.attr("href")).replace(" ", "%20");
            String linkText = a.text();
            a.replaceWith(new TextNode("[" + linkText + "](" + href + ")"));
        }
        return element.wholeText();
    }
}