package com.hotelreservation.controller.admin;

import com.hotelreservation.util.SceneNavigator;
import javafx.fxml.FXML;

public class AdminNavigationController {

    @FXML
    private void openDashboard() {
        SceneNavigator.switchTo("/views/admin/AdminDashboardView.fxml");
    }

    @FXML
    private void openReservations() {
        SceneNavigator.switchTo("/views/admin/ReservationManagementView.fxml");
    }

    @FXML
    private void openRooms() {
        SceneNavigator.switchTo("/views/admin/RoomManagementView.fxml");
    }

    @FXML
    private void openGuests() {
        SceneNavigator.switchTo("/views/admin/GuestManagementView.fxml");
    }

    @FXML
    private void openBillingPayments() {
        SceneNavigator.switchTo("/views/admin/BillingPaymentView.fxml");
    }

    @FXML
    private void openDiscounts() {
        SceneNavigator.switchTo("/views/admin/DiscountManagementView.fxml");
    }

    @FXML
    private void openLoyalty() {
        SceneNavigator.switchTo("/views/admin/LoyaltyAccountView.fxml");
    }

    @FXML
    private void openWaitlist() {
        SceneNavigator.switchTo("/views/admin/WaitlistView.fxml");
    }

    @FXML
    private void openFeedback() {
        SceneNavigator.switchTo("/views/admin/FeedbackManagementView.fxml");
    }

    @FXML
    private void openReports() {
        SceneNavigator.switchTo("/views/admin/ReportsView.fxml");
    }

    @FXML
    private void openNotifications() {
        SceneNavigator.switchTo("/views/admin/NotificationsView.fxml");
    }

    @FXML
    private void logout() {
        SceneNavigator.switchTo("/views/kiosk/WelcomeView.fxml");
    }
}