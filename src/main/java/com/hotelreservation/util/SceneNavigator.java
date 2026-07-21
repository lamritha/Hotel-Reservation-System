package com.hotelreservation.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneNavigator {

    private static Stage mainStage;

    private SceneNavigator() {
        // Prevent object creation
    }

    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    public static void switchTo(String fxmlPath) {
        if (mainStage == null) {
            throw new IllegalStateException("Main stage has not been set.");
        }

        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();

            mainStage.getScene().setRoot(root);
            mainStage.show();

        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: " + fxmlPath, e);
        }
    }
}
