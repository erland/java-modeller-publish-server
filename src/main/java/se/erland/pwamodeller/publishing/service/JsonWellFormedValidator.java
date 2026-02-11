package se.erland.pwamodeller.publishing.service;

import jakarta.json.Json;
import jakarta.json.stream.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Streaming JSON well-formedness check using JSON-P parser. */
public final class JsonWellFormedValidator {
    private JsonWellFormedValidator() {}

    public static void validateJsonFile(Path path, long maxBytes) throws IOException {
        long size = Files.size(path);
        if (size > maxBytes) {
            throw new IllegalArgumentException("JSON file too large: " + path.getFileName() + " (" + size + " bytes), max=" + maxBytes);
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             JsonParser parser = Json.createParser(reader)) {
            while (parser.hasNext()) {
                parser.next();
            }
        }
    }
}
