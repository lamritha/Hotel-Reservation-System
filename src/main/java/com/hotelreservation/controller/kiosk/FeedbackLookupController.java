package com.hotelreservation.controller.kiosk;

import com.hotelreservation.service.FeedbackService;
import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class FeedbackLookupController {

    private final FeedbackService feedbackService;

    @FXML
    private TextField lookupField;

    @FXML
    private Label statusLabel;

    public FeedbackLookupController(
            FeedbackService feedbackService
    ) {
        this.feedbackService = feedbackService;
    }

    @FXML
    private void checkFeedbackEligibility() {
        FeedbackService.FeedbackEligibility eligibility =
                feedbackService.checkEligibility(
                        lookupField.getText()
                );

        statusLabel.setText(eligibility.message());
        if (!eligibility.eligible()) {
            showError(eligibility.message());
            return;
        }

        BookingSession.setFeedbackReservationId(
                "RES-"
                        + eligibility.reservation()
                        .getReservationId()
        );
        SceneNavigator.switchTo(
                "/views/kiosk/FeedbackView.fxml"
        );
    }

    @FXML
    private void backToWelcome() {
        SceneNavigator.switchTo(
                "/views/kiosk/WelcomeView.fxml"
        );
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Feedback Not Available");
        alert.setHeaderText("Eligibility check failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
