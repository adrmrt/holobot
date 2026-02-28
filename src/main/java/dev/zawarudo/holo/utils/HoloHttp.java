package dev.zawarudo.holo.utils;

import com.google.gson.*;
import dev.zawarudo.holo.utils.exceptions.HttpStatusException;
import dev.zawarudo.holo.utils.exceptions.HttpTransportException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class HoloHttp {

    public static final String DEFAULT_USER_AGENT =
            "HoloBot/" + VersionInfo.getBotVersion() + " (+https://github.com/adrmrt/holobot) Java/" + VersionInfo.getJavaVersion();
    private static final Gson GSON = new Gson();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Map<String, HoloRateLimiter> HOST_LIMITERS = new ConcurrentHashMap<>();

    private HoloHttp() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull String getString(@NotNull String url) throws HttpStatusException, HttpTransportException {
        return getString(url, null);
    }

    public static @NotNull String getString(@NotNull String url, @Nullable Map<String, String> headers)
            throws HttpStatusException, HttpTransportException {
        HttpResponse<String> res = sendGetRequest(url, headers);
        ensure2xx(url, res);
        return res.body() == null ? "" : res.body();
    }

    /**
     * Reads a single line from a given URL.
     */
    public static @NotNull String readLine(@NotNull String url) throws HttpStatusException, HttpTransportException {
        String body = getString(url);
        int i = body.indexOf('\n');
        return i < 0 ? body : body.substring(0, i);
    }

    public static @NotNull JsonObject getJsonObject(@NotNull String url) throws HttpStatusException, HttpTransportException {
        return JsonParser.parseString(getString(url)).getAsJsonObject();
    }

    public static @NotNull JsonObject getJsonObject(@NotNull String url, @Nullable Map<String, String> headers)
            throws HttpStatusException, HttpTransportException {
        return JsonParser.parseString(getString(url, headers)).getAsJsonObject();
    }

    public static @NotNull JsonArray getJsonArray(@NotNull String url) throws HttpStatusException, HttpTransportException {
        return JsonParser.parseString(getString(url)).getAsJsonArray();
    }

    public static <T> @NotNull T getJson(@NotNull String url, @NotNull Class<T> clazz) throws HttpStatusException, HttpTransportException {
        String body = getString(url);
        return GSON.fromJson(body, clazz);
    }

    public static @NotNull JsonObject postJsonObject(@NotNull String url, @NotNull JsonObject jsonBody, @Nullable Map<String, String> headers) throws HttpStatusException, HttpTransportException {
        String body = postString(url, jsonBody.toString(), headers);
        return JsonParser.parseString(body).getAsJsonObject();
    }

    public static @NotNull String postString(@NotNull String url, @NotNull String body, @Nullable Map<String, String> headers) throws HttpStatusException, HttpTransportException {
        HttpResponse<String> res = sendPostRequest(url, body, headers);
        ensure2xx(url, res);
        return res.body() == null ? "" : res.body();
    }

    private static @NotNull HttpResponse<String> sendGetRequest(@NotNull String url, @Nullable Map<String, String> headers) throws HttpTransportException {
        final URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new HttpTransportException("Invalid URL: " + url, e);
        }

        String host = uri.getHost();
        if (host != null) {
            HoloRateLimiter limiter = HOST_LIMITERS.get(host);
            if (limiter != null) {
                limiter.acquire();
            }
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "application/json, text/plain;q=0.9, */*;q=0.8");

        if (headers != null) {
            headers.forEach(builder::header);
        }

        HttpRequest req = builder.build();

        try {
            return CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new HttpTransportException("I/O error while requesting " + uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpTransportException("Interrupted while requesting " + uri, e);
        }
    }

    private static @NotNull HttpResponse<String> sendPostRequest(@NotNull String url, @NotNull String body, @Nullable Map<String, String> headers) throws HttpTransportException {
        final URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new HttpTransportException("Invalid URL: " + url, e);
        }

        String host = uri.getHost();
        if (host != null) {
            HoloRateLimiter limiter = HOST_LIMITERS.get(host);
            if (limiter != null) {
                limiter.acquire();
            }
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=utf-8");

        if (headers != null) {
            headers.forEach(builder::header);
        }

        HttpRequest req = builder.build();

        try {
            return CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new HttpTransportException("I/O error while requesting " + uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpTransportException("Interrupted while requesting " + uri, e);
        }
    }

    private static void ensure2xx(@NotNull String url, @NotNull HttpResponse<String> res) throws HttpStatusException {
        int code = res.statusCode();
        if (code >= 200 && code < 300) return;

        throw new HttpStatusException(url, code, snippet(res.body(), 400));
    }

    private static @NotNull String snippet(@Nullable String s, int max) {
        if (s == null || s.isBlank()) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }

    public static void setHostRateLimit(@NotNull String host, @NotNull HoloRateLimiter limiter) {
        HOST_LIMITERS.put(Objects.requireNonNull(host), Objects.requireNonNull(limiter));
    }

    public static void clearHostRateLimits() {
        HOST_LIMITERS.clear();
    }
}
