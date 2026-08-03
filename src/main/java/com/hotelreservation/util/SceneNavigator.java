package com.hotelreservation.util;

import com.hotelreservation.config.AppConfig;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneNavigator {

    private static Stage mainStage;
    private static String currentFxmlPath;

    private SceneNavigator() {
    }

    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    public static void switchTo(String fxmlPath) {
        if (mainStage == null) {
            throw new IllegalStateException(
                    "Main stage has not been set."
            );
        }

        String previousFxmlPath = currentFxmlPath;
        currentFxmlPath = fxmlPath;

        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneNavigator.class.getResource(fxmlPath)
            );

            loader.setControllerFactory(AppConfig::createController);

            Parent root = loader.load();

            mainStage.getScene().setRoot(root);
            mainStage.show();

        } catch (IOException exception) {
            currentFxmlPath = previousFxmlPath;
            throw new RuntimeException(
                    "Failed to load view: " + fxmlPath,
                    exception
            );
        }
    }

    public static String getCurrentFxmlPath() {
        return currentFxmlPath;
    }
}
