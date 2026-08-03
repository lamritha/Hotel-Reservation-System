package com.hotelreservation.controller.kiosk;

import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.RulesDialog;
import com.hotelreservation.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

public class WelcomeController {

    @FXML
    private Button rulesButton;

    @FXML
    private void startNewBooking() {
        BookingSession.reset();
        SceneNavigator.switchTo("/views/kiosk/OccupancyView.fxml");
    }

    @FXML
    private void openAdminLogin() {
        SceneNavigator.switchTo("/views/admin/AdminLoginView.fxml");
    }

    @FXML
    private void openFeedbackLookup() {
        SceneNavigator.switchTo("/views/kiosk/FeedbackLookupView.fxml");
    }

    @FXML
    private void showRulesAndRegulations() {
        RulesDialog.show();
    }
}
