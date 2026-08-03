package com.hotelreservation.controller.kiosk;

import com.hotelreservation.model.Feedback;
import com.hotelreservation.service.FeedbackService;
import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class FeedbackController {

    private final FeedbackService feedbackService;

    @FXML
    private Label reservationIdLabel;

    @FXML
    private ComboBox<Integer> ratingComboBox;

    @FXML
    private TextArea commentArea;

    @FXML
    private Label sentimentLabel;

    public FeedbackController(
            FeedbackService feedbackService
    ) {
        this.feedbackService = feedbackService;
    }

    @FXML
    private void initialize() {
        reservationIdLabel.setText(
                BookingSession.getFeedbackReservationId()
        );
        ratingComboBox.setItems(
                FXCollections.observableArrayList(
                        1, 2, 3, 4, 5
                )
        );
        sentimentLabel.setText(
                "Sentiment Tag: Assigned after submission"
        );
    }

    @FXML
    private void submitFeedback() {
        Integer rating = ratingComboBox.getValue();
        if (rating == null) {
            showError("Please select a rating from 1 to 5.");
            return;
        }

        try {
            Feedback feedback = feedbackService.submit(
                    parseReservationId(),
                    rating,
                    commentArea.getText()
            );

            sentimentLabel.setText(
                    "Sentiment Tag: "
                            + feedback.getSentimentTag()
            );

            Alert alert = new Alert(
                    Alert.AlertType.INFORMATION
            );
            alert.setTitle("Feedback Submitted");
            alert.setHeaderText(
                    "Thank you for your feedback"
            );
            alert.setContentText(
                    "Your feedback was saved successfully."
            );
            alert.showAndWait();

        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void backToFeedbackLookup() {
        SceneNavigator.switchTo(
                "/views/kiosk/FeedbackLookupView.fxml"
        );
    }

    @FXML
    private void finishFeedback() {
        SceneNavigator.switchTo(
                "/views/kiosk/WelcomeView.fxml"
        );
    }

    private Long parseReservationId() {
        String value =
                BookingSession.getFeedbackReservationId()
                        .trim()
                        .toUpperCase();
        if (value.startsWith("RES-")) {
            value = value.substring(4);
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Reservation identifier is invalid."
            );
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Feedback");
        alert.setHeaderText("Feedback could not be saved");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
