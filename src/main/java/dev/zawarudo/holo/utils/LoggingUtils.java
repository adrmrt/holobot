package dev.zawarudo.holo.utils;

import org.slf4j.MDC;

import java.util.Map;

public final class LoggingUtils {

    private LoggingUtils() {
    }

    public static Runnable withMdc(Map<String, String> ctx, Runnable task) {
        return () -> {
            Map<String, String> prev = MDC.getCopyOfContextMap();
            try {
                if (ctx != null) MDC.setContextMap(ctx);
                task.run();
            } finally {
                if (prev != null) MDC.setContextMap(prev);
                else MDC.clear();
            }
        };
    }
}
