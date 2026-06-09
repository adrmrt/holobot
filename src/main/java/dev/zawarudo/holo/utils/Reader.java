package dev.zawarudo.holo.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class Reader {

    private static final Gson GSON = new Gson();

    private Reader() {
    }

    public static JsonObject readJsonObject(String filepath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filepath), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonObject.class);
        }
    }

    public static JsonObject readJsonObjectResource(String resourcePath) throws IOException {
        try (InputStreamReader r = openResourceReader(resourcePath)) {
            return GSON.fromJson(r, JsonObject.class);
        }
    }

    public static JsonArray readJsonArray(String filepath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filepath), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonArray.class);
        }
    }

    public static JsonArray readJsonArrayResource(String resourcePath) throws IOException {
        try (InputStreamReader r = openResourceReader(resourcePath)) {
            return GSON.fromJson(r, JsonArray.class);
        }
    }

    public static boolean exists(String filePath) {
        return Files.exists((Paths.get(filePath)));
    }

    private static InputStreamReader openResourceReader(String resourcePath) throws IOException {
        InputStream in = Reader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new FileNotFoundException("Classpath resource not found: " + resourcePath);
        }
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }
}
