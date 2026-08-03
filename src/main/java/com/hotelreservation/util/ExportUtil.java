package com.hotelreservation.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ExportUtil {

    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int PDF_ROWS_PER_PAGE = 38;

    private ExportUtil() {
    }

    public static Path writeCsv(
            String baseName,
            List<String> headers,
            List<List<String>> rows
    ) {
        Path path = createPath(baseName, "csv");
        StringBuilder output = new StringBuilder();
        output.append(csvLine(headers)).append('\n');
        for (List<String> row : rows) {
            output.append(csvLine(row)).append('\n');
        }
        writeText(path, output.toString());
        return path;
    }

    public static Path writeTxt(
            String baseName,
            List<String> headers,
            List<List<String>> rows
    ) {
        Path path = createPath(baseName, "txt");
        StringBuilder output = new StringBuilder();
        output.append(String.join(" | ", headers))
                .append(System.lineSeparator());
        for (List<String> row : rows) {
            output.append(String.join(" | ", row))
                    .append(System.lineSeparator());
        }
        writeText(path, output.toString());
        return path;
    }

    /**
     * Writes a dependency-free, landscape Letter PDF containing a simple
     * tabular text representation. This keeps report exports portable.
     */
    public static Path writePdf(
            String baseName,
            String title,
            List<String> headers,
            List<List<String>> rows
    ) {
        Path path = createPath(baseName, "pdf");

        List<String> tableLines = new ArrayList<>();
        tableLines.add(String.join(" | ", headers));
        for (List<String> row : rows) {
            tableLines.add(String.join(" | ", row));
        }

        int pageCount = Math.max(
                1,
                (int) Math.ceil(
                        tableLines.size()
                                / (double) PDF_ROWS_PER_PAGE
                )
        );
        int fontObjectId = 3 + (pageCount * 2);
        int objectCount = fontObjectId;

        List<String> objects = new ArrayList<>();
        for (int index = 0;
             index <= objectCount;
             index++) {
            objects.add(null);
        }

        objects.set(
                1,
                "<< /Type /Catalog /Pages 2 0 R >>"
        );

        StringBuilder kids = new StringBuilder();
        for (int pageIndex = 0;
             pageIndex < pageCount;
             pageIndex++) {
            int pageObjectId = 3 + pageIndex * 2;
            int contentObjectId = pageObjectId + 1;
            kids.append(pageObjectId).append(" 0 R ");

            objects.set(
                    pageObjectId,
                    "<< /Type /Page /Parent 2 0 R "
                            + "/MediaBox [0 0 792 612] "
                            + "/Resources << /Font << /F1 "
                            + fontObjectId
                            + " 0 R >> >> "
                            + "/Contents "
                            + contentObjectId
                            + " 0 R >>"
            );

            int from = pageIndex * PDF_ROWS_PER_PAGE;
            int to = Math.min(
                    tableLines.size(),
                    from + PDF_ROWS_PER_PAGE
            );
            String content = buildPdfContent(
                    title,
                    pageIndex + 1,
                    pageCount,
                    tableLines.subList(from, to)
            );
            int length = content.getBytes(
                    StandardCharsets.ISO_8859_1
            ).length;
            objects.set(
                    contentObjectId,
                    "<< /Length " + length + " >>\n"
                            + "stream\n"
                            + content
                            + "\nendstream"
            );
        }

        objects.set(
                2,
                "<< /Type /Pages /Count "
                        + pageCount
                        + " /Kids ["
                        + kids
                        + "] >>"
        );
        objects.set(
                fontObjectId,
                "<< /Type /Font /Subtype /Type1 "
                        + "/BaseFont /Courier >>"
        );

        try {
            ByteArrayOutputStream bytes =
                    new ByteArrayOutputStream();
            writePdfBytes(
                    bytes,
                    "%PDF-1.4\n%HotelReport\n"
            );

            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);
            for (int objectId = 1;
                 objectId <= objectCount;
                 objectId++) {
                offsets.add(bytes.size());
                writePdfBytes(
                        bytes,
                        objectId + " 0 obj\n"
                                + objects.get(objectId)
                                + "\nendobj\n"
                );
            }

            int xrefOffset = bytes.size();
            writePdfBytes(
                    bytes,
                    "xref\n0 "
                            + (objectCount + 1)
                            + "\n"
            );
            writePdfBytes(
                    bytes,
                    "0000000000 65535 f \n"
            );
            for (int objectId = 1;
                 objectId <= objectCount;
                 objectId++) {
                writePdfBytes(
                        bytes,
                        String.format(
                                "%010d 00000 n \n",
                                offsets.get(objectId)
                        )
                );
            }
            writePdfBytes(
                    bytes,
                    "trailer\n<< /Size "
                            + (objectCount + 1)
                            + " /Root 1 0 R >>\n"
                            + "startxref\n"
                            + xrefOffset
                            + "\n%%EOF\n"
            );

            Files.write(path, bytes.toByteArray());
            return path;

        } catch (IOException exception) {
            throw new RuntimeException(
                    "Unable to export PDF report.",
                    exception
            );
        }
    }

    private static String buildPdfContent(
            String title,
            int page,
            int pageCount,
            List<String> lines
    ) {
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 9 Tf\n36 576 Td\n")
                .append("12 TL\n")
                .append('(')
                .append(pdfEscape(
                        title + " - Page "
                                + page + " of " + pageCount
                ))
                .append(") Tj\nT*\n");

        for (String line : lines) {
            String compact = ascii(line);
            if (compact.length() > 118) {
                compact = compact.substring(0, 115) + "...";
            }
            content.append('(')
                    .append(pdfEscape(compact))
                    .append(") Tj\nT*\n");
        }
        content.append("ET");
        return content.toString();
    }

    private static Path createPath(
            String baseName,
            String extension
    ) {
        try {
            Path directory = Path.of("exports");
            Files.createDirectories(directory);

            String safeName = baseName == null
                    ? "report"
                    : baseName.replaceAll(
                    "[^A-Za-z0-9_-]",
                    "_"
            );
            return directory.resolve(
                    safeName
                            + "-"
                            + LocalDateTime.now()
                            .format(FILE_TIME)
                            + "."
                            + extension
            );
        } catch (IOException exception) {
            throw new RuntimeException(
                    "Unable to create export directory.",
                    exception
            );
        }
    }

    private static String csvLine(List<String> values) {
        return values.stream()
                .map(ExportUtil::csvValue)
                .reduce(
                        (left, right) -> left + "," + right
                )
                .orElse("");
    }

    private static String csvValue(String value) {
        String safe = value == null ? "" : value;
        return "\""
                + safe.replace("\"", "\"\"")
                + "\"";
    }

    private static void writeText(
            Path path,
            String value
    ) {
        try {
            Files.writeString(
                    path,
                    value,
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new RuntimeException(
                    "Unable to export report.",
                    exception
            );
        }
    }

    private static void writePdfBytes(
            ByteArrayOutputStream output,
            String value
    ) throws IOException {
        output.write(
                value.getBytes(
                        StandardCharsets.ISO_8859_1
                )
        );
    }

    private static String pdfEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private static String ascii(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
}
