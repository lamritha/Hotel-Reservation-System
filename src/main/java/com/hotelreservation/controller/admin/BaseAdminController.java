package com.hotelreservation.controller.admin;

import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public abstract class BaseAdminController {

    protected final AuthenticationService authenticationService;
    protected final AdminSession adminSession;

    protected BaseAdminController(
            AuthenticationService authenticationService,
            AdminSession adminSession
    ) {
        this.authenticationService = authenticationService;
        this.adminSession = adminSession;
    }

    @FXML
    protected void openDashboard() {
        openProtectedView(
                "/views/admin/AdminDashboardView.fxml"
        );
    }

    @FXML
    protected void openReservations() {
        openProtectedView(
                "/views/admin/ReservationManagementView.fxml"
        );
    }

    @FXML
    protected void openRooms() {
        openProtectedView(
                "/views/admin/RoomManagementView.fxml"
        );
    }

    @FXML
    protected void openGuests() {
        openProtectedView(
                "/views/admin/GuestManagementView.fxml"
        );
    }

    @FXML
    protected void openBillingPayments() {
        openProtectedView(
                "/views/admin/BillingPaymentView.fxml"
        );
    }

    @FXML
    protected void openDiscounts() {
        openProtectedView(
                "/views/admin/DiscountManagementView.fxml"
        );
    }

    @FXML
    protected void openLoyalty() {
        openProtectedView(
                "/views/admin/LoyaltyAccountView.fxml"
        );
    }

    @FXML
    protected void openWaitlist() {
        openProtectedView(
                "/views/admin/WaitlistView.fxml"
        );
    }

    @FXML
    protected void openFeedback() {
        openProtectedView(
                "/views/admin/FeedbackManagementView.fxml"
        );
    }

    @FXML
    protected void openReports() {
        openProtectedView(
                "/views/admin/ReportsView.fxml"
        );
    }

    @FXML
    protected void openNotifications() {
        openProtectedView(
                "/views/admin/NotificationsView.fxml"
        );
    }

    protected void openProtectedView(String fxmlPath) {
        if (!adminSession.isAuthenticated()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Authentication Required");
            alert.setHeaderText("Administrator session required");
            alert.setContentText(
                    "Please log in before opening administrative screens."
            );
            alert.showAndWait();

            SceneNavigator.switchTo(
                    "/views/admin/AdminLoginView.fxml"
            );
            return;
        }

        SceneNavigator.switchTo(fxmlPath);
    }

    @FXML
    protected void logout() {
        authenticationService.logout();

        SceneNavigator.switchTo(
                "/views/kiosk/WelcomeView.fxml"
        );
    }
}