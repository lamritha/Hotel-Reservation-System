package com.hotelreservation.controller.admin;

import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class AdminNavigationController
        extends BaseAdminController {

    private static final String ACTIVE_STYLE =
            "-fx-background-color: #2563eb; "
                    + "-fx-text-fill: white; "
                    + "-fx-font-weight: bold;";

    @FXML
    private Button dashboardButton;
    @FXML
    private Button reservationsButton;
    @FXML
    private Button roomsButton;
    @FXML
    private Button guestsButton;
    @FXML
    private Button billingButton;
    @FXML
    private Button discountsButton;
    @FXML
    private Button loyaltyButton;
    @FXML
    private Button waitlistButton;
    @FXML
    private Button feedbackButton;
    @FXML
    private Button reportsButton;
    @FXML
    private Button notificationsButton;

    public AdminNavigationController(
            AuthenticationService authenticationService,
            AdminSession adminSession
    ) {
        super(authenticationService, adminSession);
    }

    @FXML
    private void initialize() {
        String path = SceneNavigator.getCurrentFxmlPath();
        if (path == null) {
            return;
        }

        Button activeButton = switch (path) {
            case "/views/admin/AdminDashboardView.fxml" ->
                    dashboardButton;
            case "/views/admin/ReservationManagementView.fxml" ->
                    reservationsButton;
            case "/views/admin/RoomManagementView.fxml" ->
                    roomsButton;
            case "/views/admin/GuestManagementView.fxml" ->
                    guestsButton;
            case "/views/admin/BillingPaymentView.fxml" ->
                    billingButton;
            case "/views/admin/DiscountManagementView.fxml" ->
                    discountsButton;
            case "/views/admin/LoyaltyAccountView.fxml" ->
                    loyaltyButton;
            case "/views/admin/WaitlistView.fxml" ->
                    waitlistButton;
            case "/views/admin/FeedbackManagementView.fxml" ->
                    feedbackButton;
            case "/views/admin/ReportsView.fxml" ->
                    reportsButton;
            case "/views/admin/NotificationsView.fxml" ->
                    notificationsButton;
            default -> null;
        };

        if (activeButton != null) {
            activeButton.setStyle(ACTIVE_STYLE);
        }
    }
}
