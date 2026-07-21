package com.hotelreservation.controller.kiosk;

import com.hotelreservation.entity.LoyaltyAccount;
import com.hotelreservation.repository.LoyaltyAccountRepository;
import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.SceneNavigator;
import com.hotelreservation.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class LoyaltyCheckController {

    @FXML
    private Button rulesButton;

    @FXML
    private TextField phoneField;

    @FXML
    private Label loyaltyStatusLabel;

    @FXML
    private Label loyaltyPointsLabel;

    @FXML
    private Hyperlink createLoyaltyHyperlink;

    private final LoyaltyAccountRepository loyaltyAccountRepository = new LoyaltyAccountRepository();

    @FXML
    private void initialize() {
        phoneField.setText(BookingSession.getPhone());
        loyaltyStatusLabel.setText("Phone number loaded from guest details.");
        loyaltyPointsLabel.setText("Click Check Loyalty to check available points.");
        hideCreateLoyaltyLink();
    }

    @FXML
    private void checkLoyalty() {
        String phone = phoneField.getText().trim();

        if (phone.isEmpty()) {
            showError("Please enter a phone number.");
            return;
        }

        BookingSession.setPhone(phone);

        loyaltyAccountRepository.findByGuestPhone(phone).ifPresentOrElse(
                this::showLoyaltyAccountFound,
                this::showLoyaltyAccountMissing
        );
    }

    private void showLoyaltyAccountFound(LoyaltyAccount account) {
        BookingSession.setLoyaltyEnrolled(true);
        BookingSession.setLoyaltyNumber(account.getLoyaltyNumber());
        BookingSession.setLoyaltyPointsBalance(account.getPointsBalance());
        loyaltyStatusLabel.setText("Loyalty account found: " + account.getLoyaltyNumber());
        loyaltyPointsLabel.setText("Available Loyalty Points: " + account.getPointsBalance());
        hideCreateLoyaltyLink();
    }

    private void showLoyaltyAccountMissing() {
        BookingSession.setLoyaltyEnrolled(false);
        BookingSession.setLoyaltyNumber("");
        BookingSession.setLoyaltyPointsBalance(0);
        loyaltyStatusLabel.setText("No loyalty account found for this guest.");
        loyaltyPointsLabel.setText("Guest can continue without loyalty points.");
        createLoyaltyHyperlink.setVisible(true);
        createLoyaltyHyperlink.setManaged(true);
    }

    private void hideCreateLoyaltyLink() {
        createLoyaltyHyperlink.setVisible(false);
        createLoyaltyHyperlink.setManaged(false);
    }

    @FXML
    private void createLoyaltyAccountPlaceholder() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Loyalty Signup");
        alert.setHeaderText("Create a loyalty account");
        alert.setContentText("This would open account signup.");
        alert.showAndWait();
    }

    @FXML
    private void continueToBookingSummary() {
        String phone = phoneField.getText().trim();

        if (!ValidationUtil.isValidPhone(phone)) {
            showError("Please enter a valid phone number.");
            return;
        }

        BookingSession.setPhone(phone);

        System.out.println("Loyalty check completed:");
        System.out.println("Phone: " + BookingSession.getPhone());

        SceneNavigator.switchTo("/views/kiosk/BookingSummaryView.fxml");
    }

    @FXML
    private void backToAddOns() {
        SceneNavigator.switchTo("/views/kiosk/AddOnsView.fxml");
    }

    @FXML
    private void showRulesAndRegulations() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rules & Regulations");
        alert.setHeaderText("Hotel Rules & Regulations");
        alert.setContentText("Placeholder: rules and regulations content will be added later.");
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Phone Required");
        alert.setHeaderText("Please check the phone number");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
