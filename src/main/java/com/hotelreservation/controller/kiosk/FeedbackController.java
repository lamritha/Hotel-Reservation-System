package com.hotelreservation.controller.kiosk;

import com.hotelreservation.util.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class FeedbackController {

    @FXML
    private Label reservationIdLabel;

    @FXML
    private ComboBox<Integer> ratingComboBox;

    @FXML
    private TextArea commentArea;

    @FXML
    private Label sentimentLabel;

    @FXML
    private void initialize() {
        reservationIdLabel.setText("RES-1001");

        ratingComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));

        sentimentLabel.setText("Sentiment Tag: Pending Analysis");
    }

    @FXML
    private void submitFeedback() {
        Integer rating = ratingComboBox.getValue();
        String comment = commentArea.getText().trim();

        if (rating == null) {
            showError("Please select a rating from 1 to 5.");
            return;
        }

        if (comment.isEmpty()) {
            showError("Please enter a feedback comment.");
            return;
        }

        String sentiment;

        if (rating >= 4) {
            sentiment = "Positive";
        } else if (rating == 3) {
            sentiment = "Neutral";
        } else {
            sentiment = "Needs Review";
        }

        sentimentLabel.setText("Sentiment Tag: " + sentiment);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Feedback Submitted");
        alert.setHeaderText("Thank you for your feedback");
        alert.setContentText("Your feedback has been submitted successfully.");
        alert.showAndWait();
    }

    @FXML
    private void backToFeedbackLookup() {
        SceneNavigator.switchTo("/views/kiosk/FeedbackLookupView.fxml");
    }

    @FXML
    private void finishFeedback() {
        SceneNavigator.switchTo("/views/kiosk/WelcomeView.fxml");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Feedback");
        alert.setHeaderText("Please check the feedback form");
        alert.setContentText(message);
        alert.showAndWait();
    }
}