package com.hotelreservation;

import com.hotelreservation.util.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        SceneNavigator.setMainStage(stage);

        stage.setTitle("Hotel Reservation System");
        stage.setResizable(true);
        stage.setMaximized(true);

        SceneNavigator.switchTo("/views/kiosk/WelcomeView.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}