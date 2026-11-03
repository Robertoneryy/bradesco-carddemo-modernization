package com.carddemo.batch.interest.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/** Reads and writes fixed-width records stored one per line (the layout of the files in app/data/ASCII). */
public final class RecordFiles {

    private RecordFiles() {
    }

    public static <T> List<T> read(Path path, Function<String, T> parser) {
        try {
            return Files.readAllLines(path, StandardCharsets.US_ASCII).stream()
                    .map(line -> line.replace("\r", ""))
                    .filter(line -> !line.isBlank())
                    .map(parser)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        }
    }

    public static <T> void write(Path path, List<T> records, Function<T, String> formatter) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, records.stream().map(formatter).toList(), StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + path, e);
        }
    }
}
