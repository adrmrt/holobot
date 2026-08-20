package dev.zawarudo.holo.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.exceptions.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <a href="https://dictionaryapi.com/products/json">Documentation of the API responses</a>
 */
public final class MerriamWebsterClient {

    public enum Product {
        DICTIONARY("collegiate"),
        THESAURUS("thesaurus");

        private final String path;

        Product(String path) {
            this.path = path;
        }
    }

    private static final String BASE = "https://www.dictionaryapi.com/api/v3/references/";

    private final String dictionaryKey;
    private final String thesaurusKey;

    public MerriamWebsterClient(String dictionaryKey, String thesaurusKey) {
        this.dictionaryKey = Objects.requireNonNull(dictionaryKey, "dictionaryKey must not be null");
        this.thesaurusKey = Objects.requireNonNull(thesaurusKey, "thesaurusKey must not be null");
    }

    public LookupResult lookupDictionary(@NotNull String term) throws APIException, NotFoundException, InvalidRequestException {
        return lookup(Product.DICTIONARY, term, dictionaryKey);
    }

    public LookupResult lookupThesaurus(@NotNull String term) throws APIException, NotFoundException, InvalidRequestException {
        return lookup(Product.THESAURUS, term, thesaurusKey);
    }

    public record LookupResult(List<Entry> entries, List<String> suggestions) {
        public LookupResult {
            entries = entries == null ? List.of() : List.copyOf(entries);
            suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
        }

        public boolean hasEntries() {
            return !entries.isEmpty();
        }

        public boolean hasSuggestions() {
            return !suggestions.isEmpty();
        }
    }

    public record Entry(
        String id,
        String headword,
        String functionalLabel,
        String pronunciation,
        String plural,
        List<String> shortDefs,
        List<String> synonyms,
        List<String> antonyms,
        List<String> examples,
        String etymology,
        String usageNotes,
        boolean offensive
    ) {
        public Entry {
            headword = headword == null ? null : headword.replace("*", "·");
            plural = plural == null ? null : plural.replace("*", "·");

            shortDefs = shortDefs == null ? List.of() : List.copyOf(shortDefs);
            synonyms = synonyms == null ? List.of() : List.copyOf(synonyms);
            antonyms = antonyms == null ? List.of() : List.copyOf(antonyms);
            examples = examples == null ? List.of() : List.copyOf(examples);
        }
    }

    private LookupResult lookup(Product product, @NotNull String term, String key) throws APIException, NotFoundException, InvalidRequestException {
        String q = term.trim();
        if (q.isBlank()) {
            return new LookupResult(List.of(), List.of());
        }

        String url = BASE + product.path + "/json/" + encodePath(q) + "?key=" + encodeQuery(key);

        final JsonArray array;
        try {
            array = HoloHttp.getJsonArray(url);
        } catch (HttpStatusException ex) {
            mapHttpStatus(product, ex);
            throw new APIException("Unreachable", ex);
        } catch (HttpTransportException ex) {
            throw new APIException("Transport error while contacting Merriam-Webster (" + product.name().toLowerCase() + ").", ex);
        } catch (RuntimeException ex) {
            throw new InvalidRequestException("Unexpected response from Merriam-Webster (" + product.name().toLowerCase() + "). " + "Check your API key configuration.", ex);
        }

        if (array.isEmpty()) {
            return new LookupResult(List.of(), List.of());
        }

        JsonElement first = array.get(0);
        if (first.isJsonPrimitive()) {
            return new LookupResult(List.of(), parseSuggestions(array));
        }

        return new LookupResult(parseEntries(product, array), List.of());
    }

    private static void mapHttpStatus(Product product, HttpStatusException ex)
        throws APIException, InvalidRequestException, NotFoundException {

        int code = ex.getStatusCode();
        String name = product.name().toLowerCase();

        if (code == 404) {
            throw new NotFoundException("Merriam-Webster endpoint not found (" + name + ").", ex);
        }
        if (code == 429) {
            throw new APIException("429 Too Many Requests (Merriam-Webster " + name + " rate limit).", ex);
        }
        if (code >= 400 && code < 500) {
            throw new InvalidRequestException(code + " Client error from Merriam-Webster (" + name + ").", ex);
        }
        throw new APIException(code + " Server error from Merriam-Webster (" + name + ").", ex);
    }

    private static List<String> parseSuggestions(JsonArray array) {
        List<String> out = new ArrayList<>(Math.min(array.size(), 25));
        for (JsonElement el : array) {
            if (el != null && el.isJsonPrimitive()) {
                String s = safeString(el);
                if (!s.isBlank()) out.add(s);
            }
        }
        return out;
    }

    private static List<Entry> parseEntries(Product product, JsonArray array) {
        List<Entry> out = new ArrayList<>(Math.min(array.size(), 25));

        for (JsonElement el : array) {
            if (el == null || !el.isJsonObject()) continue;

            JsonObject obj = el.getAsJsonObject();

            JsonObject meta = obj.has("meta") && obj.get("meta").isJsonObject() ? obj.getAsJsonObject("meta") : null;
            JsonObject hwi = obj.has("hwi") && obj.get("hwi").isJsonObject() ? obj.getAsJsonObject("hwi") : null;

            String id = meta == null ? null : safeString(meta.get("id"));
            String headword = hwi == null ? null : safeString(hwi.get("hw"));
            String fl = safeString(obj.get("fl"));

            String pronunciation = extractPronunciation(hwi);
            String plural = extractPlural(obj);

            List<String> shortDefs = obj.has("shortdef") && obj.get("shortdef").isJsonArray()
                ? stringList(obj.getAsJsonArray("shortdef"))
                : List.of();

            List<String> examples = extractExamples(obj);

            List<String> syns = (product == Product.THESAURUS && meta != null)
                ? flattenNestedStringLists(meta.get("syns"))
                : List.of();

            List<String> ants = (product == Product.THESAURUS && meta != null)
                ? flattenNestedStringLists(meta.get("ants"))
                : List.of();

            String etymology = extractEtymology(obj);

            String usageNotes = extractUsageNotes(obj);

            boolean offensive = meta != null
                && meta.has("offensive")
                && meta.get("offensive").getAsBoolean();

            out.add(new Entry(
                id,
                headword,
                fl,
                pronunciation,
                plural,
                shortDefs,
                syns,
                ants,
                examples,
                etymology,
                usageNotes,
                offensive
            ));
        }

        return out;
    }

    private static List<String> extractExamples(JsonObject obj) {
        if (!obj.has("def") || !obj.get("def").isJsonArray()) return List.of();

        List<String> out = new ArrayList<>();

        for (JsonElement defEl : obj.getAsJsonArray("def")) {
            if (defEl == null || !defEl.isJsonObject()) continue;
            JsonObject defObj = defEl.getAsJsonObject();

            if (!defObj.has("sseq") || !defObj.get("sseq").isJsonArray()) continue;
            JsonArray sseq = defObj.getAsJsonArray("sseq");

            walkJson(sseq, el -> {
                if (!el.isJsonArray()) return;

                JsonArray arr = el.getAsJsonArray();
                if (arr.size() < 2) return;

                JsonElement maybeSenseObj = arr.get(1);
                if (!maybeSenseObj.isJsonObject()) return;

                JsonObject sense = maybeSenseObj.getAsJsonObject();
                if (!sense.has("dt") || !sense.get("dt").isJsonArray()) return;

                JsonArray dt = sense.getAsJsonArray("dt");
                for (JsonElement dtEl : dt) {
                    if (dtEl == null || !dtEl.isJsonArray()) continue;
                    JsonArray dtPair = dtEl.getAsJsonArray();
                    if (dtPair.size() < 2) continue;

                    String tag = safeString(dtPair.get(0));
                    if (!"vis".equals(tag)) continue;

                    JsonElement visEl = dtPair.get(1);
                    if (!visEl.isJsonArray()) continue;

                    for (JsonElement v : visEl.getAsJsonArray()) {
                        if (v != null && v.isJsonObject()) {
                            String t = Formatter.mwToDiscord(safeString(v.getAsJsonObject().get("t")));
                            if (!t.isBlank()) out.add(t);
                        }
                    }
                }
            });

        }

        if (out.isEmpty()) return List.of();

        LinkedHashSet<String> dedup = new LinkedHashSet<>(out);
        List<String> result = new ArrayList<>(dedup);
        return result.size() > 20 ? result.subList(0, 20) : result;
    }

    private static void walkJson(JsonElement root, java.util.function.Consumer<JsonElement> fn) {
        if (root == null) return;
        fn.accept(root);

        if (root.isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray()) {
                walkJson(el, fn);
            }
        } else if (root.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().entrySet()) {
                walkJson(e.getValue(), fn);
            }
        }
    }

    private static List<String> stringList(JsonArray arr) {
        List<String> out = new ArrayList<>(arr.size());
        for (JsonElement el : arr) {
            String s = Formatter.mwToDiscord(safeString(el));
            if (!s.isBlank()) out.add(s);
        }
        return out;
    }

    private static List<String> flattenNestedStringLists(@Nullable JsonElement el) {
        if (el == null || !el.isJsonArray()) return List.of();

        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (JsonElement outer : el.getAsJsonArray()) {
            if (outer != null && outer.isJsonArray()) {
                for (JsonElement inner : outer.getAsJsonArray()) {
                    String s = safeString(inner);
                    if (!s.isBlank()) out.add(s);
                }
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static String safeString(@Nullable JsonElement el) {
        if (el == null || el.isJsonNull()) return "";
        if (el.isJsonPrimitive()) return el.getAsString();
        return "";
    }

    private static String encodePath(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodeQuery(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static @Nullable String extractEtymology(JsonObject obj) {
        if (!obj.has("et") || !obj.get("et").isJsonArray()) return null;

        List<String> parts = new ArrayList<>();
        JsonArray etArr = obj.getAsJsonArray("et");

        for (JsonElement etEl : etArr) {
            if (etEl == null || !etEl.isJsonArray()) continue;

            JsonArray pair = etEl.getAsJsonArray();
            if (pair.size() < 2) continue;

            String tag = safeString(pair.get(0));
            if (!"text".equals(tag)) continue;

            String text = Formatter.mwToDiscord(safeString(pair.get(1)));
            if (!text.isBlank()) parts.add(text);
        }

        if (parts.isEmpty()) return null;

        String joined = String.join(" ", parts).trim();
        return joined.isBlank() ? null : joined;
    }

    private static @Nullable String extractPronunciation(@Nullable JsonObject hwi) {
        if (hwi == null) return null;

        JsonElement prsEl = hwi.get("prs");
        if (prsEl == null || !prsEl.isJsonArray()) return null;

        JsonArray prs = prsEl.getAsJsonArray();
        if (prs.isEmpty() || !prs.get(0).isJsonObject()) return null;

        String mw = safeString(prs.get(0).getAsJsonObject().get("mw"));
        return mw.isBlank() ? null : mw;
    }

    private static @Nullable String extractPlural(JsonObject obj) {
        JsonElement insEl = obj.get("ins");
        if (insEl == null || !insEl.isJsonArray()) return null;

        for (JsonElement el : insEl.getAsJsonArray()) {
            if (el == null || !el.isJsonObject()) continue;
            JsonObject inf = el.getAsJsonObject();

            if (!"plural".equalsIgnoreCase(safeString(inf.get("il")))) continue;

            String form = safeString(inf.get("if"));
            if (!form.isBlank()) return form;

            String ifc = safeString(inf.get("ifc"));
            return ifc.isBlank() ? null : ifc;
        }
        return null;
    }

    private static @Nullable String extractUsageNotes(JsonObject obj) {
        if (!obj.has("usages") || !obj.get("usages").isJsonArray()) return null;

        List<String> blocks = new ArrayList<>();
        JsonArray usages = obj.getAsJsonArray("usages");

        for (JsonElement uEl : usages) {
            if (uEl == null || !uEl.isJsonObject()) continue;
            JsonObject u = uEl.getAsJsonObject();

            String heading = safeString(u.get("pl"));
            heading = heading.isBlank() ? "" : Formatter.mwToDiscord(heading);

            String body = extractPtText(u.get("pt"));

            if (!body.isBlank()) {
                if (!heading.isBlank()) {
                    blocks.add("**" + heading + "**\n" + body);
                } else {
                    blocks.add(body);
                }
            }
        }

        if (blocks.isEmpty()) return null;
        String joined = String.join("\n\n", blocks).trim();
        return joined.isBlank() ? null : joined;
    }

    private static String extractPtText(@Nullable JsonElement ptEl) {
        if (ptEl == null || !ptEl.isJsonArray()) return "";

        List<String> parts = new ArrayList<>();
        for (JsonElement el : ptEl.getAsJsonArray()) {
            if (el == null || !el.isJsonArray()) continue;

            JsonArray pair = el.getAsJsonArray();
            if (pair.size() < 2) continue;

            String tag = safeString(pair.get(0));
            if (!"text".equals(tag)) continue;

            String text = Formatter.mwToDiscord(safeString(pair.get(1)));
            if (!text.isBlank()) parts.add(text);
        }

        return String.join(" ", parts).trim();
    }
}
