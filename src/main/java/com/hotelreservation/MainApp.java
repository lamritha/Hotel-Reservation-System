package com.hotelreservation;

import com.hotelreservation.util.SceneNavigator;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        SceneNavigator.setMainStage(stage);

        stage.setTitle("Hotel Reservation System");
        stage.setResizable(true);

        Scene scene = new Scene(new StackPane(), 1100, 700);
        stage.setScene(scene);
        stage.setMaximized(true);

        SceneNavigator.switchTo("/views/kiosk/WelcomeView.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
