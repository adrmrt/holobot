package dev.zawarudo.holo.utils.exceptions;

import com.google.gson.JsonArray;
import org.jetbrains.annotations.Nullable;

public class GraphQlException extends Exception {

    private final transient JsonArray errors;

    public GraphQlException(String message, @Nullable JsonArray errors) {
        super(message);
        this.errors = errors;
    }

    public @Nullable JsonArray getErrors() {
        return errors;
    }
}