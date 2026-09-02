package com.SRTS.CAT.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal .env file loader - no new dependency. A real OS environment
 * variable always wins if set; otherwise this falls back to a KEY=VALUE
 * line in a ".env" file in the working directory (never committed - see
 * .gitignore). Used by CatApplication and every standalone tool so none
 * of them need per-run-configuration environment setup.
 */
public final class EnvLoader {

    private static Map<String, String> dotEnv;

    private EnvLoader() {
    }

    public static String get(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return dotEnv().get(key);
    }

    /**
     * For Spring: copies any of the given keys into System properties if
     * they aren't already set as a real environment variable, so Spring's
     * ${KEY} placeholder resolution (application.properties) picks them up.
     */
    public static void applyToSystemProperties(String... keys) {
        for (String key : keys) {
            if (System.getenv(key) != null || System.getProperty(key) != null) {
                continue;
            }
            String value = dotEnv().get(key);
            if (value != null) {
                System.setProperty(key, value);
            }
        }
    }

    private static synchronized Map<String, String> dotEnv() {
        if (dotEnv != null) {
            return dotEnv;
        }
        dotEnv = new HashMap<>();
        Path path = Path.of(".env");
        if (!Files.exists(path)) {
            return dotEnv;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                dotEnv.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
            }
        } catch (IOException e) {
            System.err.println("Failed to read .env: " + e.getMessage());
        }
        return dotEnv;
    }
}
