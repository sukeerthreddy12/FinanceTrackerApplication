package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class DotEnvLoader {

    private DotEnvLoader() {
    }

    static void load() {
        Path envFile = findEnvFile();
        if (envFile == null) {
            return;
        }
        try {
            for (String line : Files.readAllLines(envFile)) {
                apply(line);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read " + envFile, ex);
        }
    }

    private static Path findEnvFile() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 5 && dir != null; i++) {
            Path candidate = dir.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }

    private static void apply(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }
        if (trimmed.startsWith("export ")) {
            trimmed = trimmed.substring("export ".length()).trim();
        }
        int separator = trimmed.indexOf('=');
        if (separator <= 0) {
            return;
        }
        String key = trimmed.substring(0, separator).trim();
        String value = stripQuotes(trimmed.substring(separator + 1).trim());
        System.setProperty(key, value);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
