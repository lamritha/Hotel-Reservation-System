package com.hotelreservation.controller.admin;

import com.hotelreservation.model.Reservation;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.service.DashboardService;
import com.hotelreservation.util.AlertUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;

public class AdminDashboardController
        extends BaseAdminController {

    private final DashboardService dashboardService;

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label totalReservationsLabel;
    @FXML
    private Label checkedInLabel;
    @FXML
    private Label availableRoomsLabel;
    @FXML
    private Label occupiedRoomsLabel;
    @FXML
    private Label totalGuestsLabel;
    @FXML
    private Label outstandingBillsLabel;
    @FXML
    private Label waitlistLabel;
    @FXML
    private Label notificationsLabel;
    @FXML
    private Label revenueLabel;

    @FXML
    private TableView<Reservation> recentTable;
    @FXML
    private TableColumn<Reservation, String> recentIdColumn;
    @FXML
    private TableColumn<Reservation, String> recentGuestColumn;
    @FXML
    private TableColumn<Reservation, LocalDate> recentCheckInColumn;
    @FXML
    private TableColumn<Reservation, String> recentStatusColumn;

    public AdminDashboardController(
            AuthenticationService authenticationService,
            AdminSession adminSession,
            DashboardService dashboardService
    ) {
        super(authenticationService, adminSession);
        this.dashboardService = dashboardService;
    }

    @FXML
    private void initialize() {
        recentIdColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        "RES-"
                                + data.getValue()
                                .getReservationId()
                )
        );
        recentGuestColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getGuest().getFullName()
                )
        );
        recentCheckInColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getCheckInDate()
                )
        );
        recentStatusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getStatus().name()
                                .replace('_', ' ')
                )
        );
        refreshDashboard();
    }

    @FXML
    private void refreshDashboard() {
        try {
            DashboardService.DashboardSummary summary =
                    dashboardService.getSummary();
            welcomeLabel.setText(
                    "Welcome, " + summary.adminName()
                            + " (" + summary.role() + ")"
            );
            totalReservationsLabel.setText(
                    String.valueOf(summary.totalReservations())
            );
            checkedInLabel.setText(
                    String.valueOf(
                            summary.checkedInReservations()
                    )
            );
            availableRoomsLabel.setText(
                    String.valueOf(summary.availableRooms())
            );
            occupiedRoomsLabel.setText(
                    String.valueOf(summary.occupiedRooms())
            );
            totalGuestsLabel.setText(
                    String.valueOf(summary.totalGuests())
            );
            outstandingBillsLabel.setText(
                    String.valueOf(summary.outstandingBills())
            );
            waitlistLabel.setText(
                    String.valueOf(summary.activeWaitlist())
            );
            notificationsLabel.setText(
                    String.valueOf(
                            summary.unreadNotifications()
                    )
            );
            revenueLabel.setText(
                    String.format(
                            "CAD %.2f",
                            summary.revenueToday()
                    )
            );
            recentTable.setItems(
                    FXCollections.observableArrayList(
                            summary.recentReservations()
                    )
            );
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Dashboard could not be loaded",
                    exception
            );
        }
    }
}
