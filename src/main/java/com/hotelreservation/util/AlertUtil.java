package com.hotelreservation.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class AlertUtil {

    private AlertUtil() {
    }

    public static void info(
            String title,
            String message
    ) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void error(
            String title,
            Throwable throwable
    ) {
        String message = rootMessage(throwable);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean confirm(
            String title,
            String message
    ) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                message,
                ButtonType.OK,
                ButtonType.CANCEL
        );
        alert.setTitle(title);
        alert.setHeaderText(title);
        return alert.showAndWait()
                .filter(ButtonType.OK::equals)
                .isPresent();
    }

    public static String rootMessage(Throwable throwable) {
        if (throwable == null) {
            return "An unexpected error occurred.";
        }
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }
}
