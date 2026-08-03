package com.hotelreservation.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

public final class AppLogger {

    private static final String ROOT_LOGGER_NAME = "com.hotelreservation";
    private static final String LOG_DIRECTORY = "logs";
    private static final String LOG_FILE_PATTERN =
            LOG_DIRECTORY + "/system_logs.%g.log";

    private static final int ONE_MEGABYTE = 1024 * 1024;
    private static final int LOG_FILE_COUNT = 10;

    private static final Logger ROOT_LOGGER =
            Logger.getLogger(ROOT_LOGGER_NAME);

    static {
        configure();
    }

    private AppLogger() {
    }

    private static void configure() {
        ROOT_LOGGER.setUseParentHandlers(false);
        ROOT_LOGGER.setLevel(Level.ALL);

        for (Handler existingHandler : ROOT_LOGGER.getHandlers()) {
            existingHandler.close();
            ROOT_LOGGER.removeHandler(existingHandler);
        }

        try {
            Files.createDirectories(Path.of(LOG_DIRECTORY));

            FileHandler fileHandler = new FileHandler(
                    LOG_FILE_PATTERN,
                    ONE_MEGABYTE,
                    LOG_FILE_COUNT,
                    true
            );

            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SimpleFormatter());
            ROOT_LOGGER.addHandler(fileHandler);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(new SimpleFormatter());
            ROOT_LOGGER.addHandler(consoleHandler);

        } catch (IOException | SecurityException exception) {
            ConsoleHandler fallbackHandler = new ConsoleHandler();
            fallbackHandler.setLevel(Level.ALL);
            fallbackHandler.setFormatter(new SimpleFormatter());
            ROOT_LOGGER.addHandler(fallbackHandler);

            ROOT_LOGGER.log(
                    Level.SEVERE,
                    "Failed to initialize rotating file logging.",
                    exception
            );
        }
    }

    public static Logger getLogger(Class<?> sourceClass) {
        return Logger.getLogger(sourceClass.getName());
    }

    public static void audit(
            Logger logger,
            Level level,
            String actor,
            String action,
            String entityType,
            String entityIdentifier,
            String message
    ) {
        String logMessage = String.format(
                "actor=%s | action=%s | entityType=%s | entityId=%s | message=%s",
                sanitize(actor),
                sanitize(action),
                sanitize(entityType),
                sanitize(entityIdentifier),
                sanitize(message)
        );

        logger.log(level, logMessage);
    }

    public static void exception(
            Logger logger,
            String message,
            Throwable throwable
    ) {
        logger.log(Level.SEVERE, sanitize(message), throwable);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value
                .replace('\n', ' ')
                .replace('\r', ' ');
    }
}