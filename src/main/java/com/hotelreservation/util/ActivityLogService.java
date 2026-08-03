package com.hotelreservation.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Parses the rotating java.util.logging audit files into tabular activity
 * records for the Reports screen.
 */
public class ActivityLogService {

    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile(
                    "^([A-Z][a-z]{2} \\d{1,2}, \\d{4} "
                            + "\\d{1,2}:\\d{2}:\\d{2} [AP]M) .*$"
            );

    private static final Pattern AUDIT_PATTERN =
            Pattern.compile(
                    "^[A-Z]+: actor=(.*?) \\| action=(.*?) "
                            + "\\| entityType=(.*?) \\| entityId=(.*?) "
                            + "\\| message=(.*)$"
            );

    private static final DateTimeFormatter LOG_DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "MMM d, uuuu h:mm:ss a",
                    Locale.ENGLISH
            );

    private final Path logDirectory;

    public ActivityLogService() {
        this(Path.of("logs"));
    }

    public ActivityLogService(Path logDirectory) {
        this.logDirectory = logDirectory;
    }

    public List<ActivityLogRow> readActivityLogs() {
        if (!Files.isDirectory(logDirectory)) {
            return List.of();
        }

        List<ActivityLogRow> rows = new ArrayList<>();

        try (Stream<Path> stream =
                     Files.list(logDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .startsWith("system_logs.")
                    )
                    .forEach(path ->
                            readFile(path, rows)
                    );
        } catch (IOException exception) {
            throw new RuntimeException(
                    "Unable to read activity logs.",
                    exception
            );
        }

        rows.sort(
                Comparator.comparing(
                        ActivityLogRow::timestamp
                ).reversed()
        );
        return rows;
    }

    private void readFile(
            Path path,
            List<ActivityLogRow> rows
    ) {
        LocalDateTime timestamp =
                fileTimestamp(path);
        try {
            for (String line :
                    Files.readAllLines(
                            path,
                            StandardCharsets.UTF_8
                    )) {
                Matcher timestampMatcher =
                        TIMESTAMP_PATTERN.matcher(line);
                if (timestampMatcher.matches()) {
                    try {
                        timestamp = LocalDateTime.parse(
                                timestampMatcher.group(1),
                                LOG_DATE_FORMAT
                        );
                    } catch (DateTimeParseException ignored) {
                        // Retain the last valid timestamp.
                    }
                    continue;
                }

                Matcher auditMatcher =
                        AUDIT_PATTERN.matcher(line);
                if (auditMatcher.matches()) {
                    rows.add(
                            new ActivityLogRow(
                                    timestamp,
                                    auditMatcher.group(1),
                                    auditMatcher.group(2),
                                    auditMatcher.group(3),
                                    auditMatcher.group(4),
                                    auditMatcher.group(5)
                            )
                    );
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException(
                    "Unable to read log file " + path + ".",
                    exception
            );
        }
    }

    private LocalDateTime fileTimestamp(Path path) {
        try {
            FileTime time = Files.getLastModifiedTime(path);
            return LocalDateTime.ofInstant(
                    time.toInstant(),
                    ZoneId.systemDefault()
            );
        } catch (IOException exception) {
            return LocalDateTime.now();
        }
    }

    public record ActivityLogRow(
            LocalDateTime timestamp,
            String actor,
            String action,
            String entityType,
            String entityIdentifier,
            String message
    ) {
    }
}
