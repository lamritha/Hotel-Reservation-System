package com.hotelreservation.controller.admin;

import com.hotelreservation.config.AppConfig;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.ReservationRoom;
import com.hotelreservation.model.ReservationStatus;
import com.hotelreservation.model.Room;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.service.ReservationManagementService;
import com.hotelreservation.service.BillingPaymentService;
import com.hotelreservation.util.AlertUtil;
import com.hotelreservation.util.TablePaginationSupport;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.geometry.Rectangle2D;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ReservationManagementController
        extends BaseAdminController {

    private final ReservationManagementService
            reservationManagementService;
    private final BillingPaymentService billingPaymentService;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<ReservationStatus> statusFilter;

    @FXML
    private DatePicker dateFromPicker;

    @FXML
    private DatePicker dateToPicker;

    @FXML
    private TableView<Reservation> reservationTable;

    @FXML
    private TableColumn<Reservation, String>
            reservationIdColumn;

    @FXML
    private TableColumn<Reservation, String>
            guestColumn;

    @FXML
    private TableColumn<Reservation, String>
            phoneColumn;

    @FXML
    private TableColumn<Reservation, String>
            roomsColumn;

    @FXML
    private TableColumn<Reservation, LocalDate>
            checkInColumn;

    @FXML
    private TableColumn<Reservation, LocalDate>
            checkOutColumn;

    @FXML
    private TableColumn<Reservation, ReservationStatus>
            statusColumn;

    @FXML
    private Label resultCountLabel;

    @FXML
    private Label pageLabel;

    @FXML
    private Button previousPageButton;

    @FXML
    private Button nextPageButton;

    @FXML
    private Button viewDetailsButton;

    @FXML
    private Button modifyButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button checkInButton;

    @FXML
    private Button checkOutButton;

    private TablePaginationSupport<Reservation> pagination;

    public ReservationManagementController(
            AuthenticationService authenticationService,
            AdminSession adminSession,
            ReservationManagementService
                    reservationManagementService,
            BillingPaymentService billingPaymentService
    ) {
        super(authenticationService, adminSession);

        this.reservationManagementService =
                reservationManagementService;
        this.billingPaymentService = billingPaymentService;
    }

    @FXML
    private void initialize() {
        configureColumns();
        configureStatusFilter();
        configureSelectionButtons();
        pagination = new TablePaginationSupport<>(
                reservationTable,
                resultCountLabel,
                pageLabel,
                previousPageButton,
                nextPageButton,
                10,
                "reservation found",
                "reservations found"
        );
        searchReservations();
    }

    private void configureSelectionButtons() {
        viewDetailsButton.disableProperty().bind(
                reservationTable
                        .getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );

        modifyButton.disableProperty().bind(
                reservationTable
                        .getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );

        cancelButton.disableProperty().bind(
                reservationTable
                        .getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );

        checkInButton.disableProperty().bind(
                reservationTable
                        .getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );

        checkOutButton.disableProperty().bind(
                reservationTable
                        .getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );
    }

    @FXML
    private void checkInReservation() {
        changeStayStatus(true);
    }

    @FXML
    private void checkOutReservation() {
        changeStayStatus(false);
    }

    private void changeStayStatus(boolean checkingIn) {
        Reservation reservation =
                reservationTable.getSelectionModel()
                        .getSelectedItem();
        if (reservation == null) {
            AlertUtil.error(
                    "Reservation required",
                    new IllegalArgumentException(
                            "Select a reservation first."
                    )
            );
            return;
        }

        try {
            if (checkingIn) {
                billingPaymentService.checkIn(
                        reservation.getReservationId()
                );
            } else {
                billingPaymentService.checkout(
                        reservation.getReservationId()
                );
            }

            AlertUtil.info(
                    checkingIn ? "Check In" : "Check Out",
                    checkingIn
                            ? "Guest checked in successfully."
                            : "Checkout completed successfully."
            );
            searchReservations();
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    checkingIn
                            ? "Check-in failed"
                            : "Checkout failed",
                    exception
            );
        }
    }

    private void configureColumns() {
        reservationIdColumn.setCellValueFactory(
                cellData -> new ReadOnlyStringWrapper(
                        "RES-"
                                + cellData.getValue()
                                .getReservationId()
                )
        );

        guestColumn.setCellValueFactory(
                cellData -> new ReadOnlyStringWrapper(
                        cellData.getValue()
                                .getGuest()
                                .getFullName()
                )
        );

        phoneColumn.setCellValueFactory(
                cellData -> new ReadOnlyStringWrapper(
                        cellData.getValue()
                                .getGuest()
                                .getPhone()
                )
        );

        roomsColumn.setCellValueFactory(
                cellData -> new ReadOnlyStringWrapper(
                        buildRoomPlan(cellData.getValue())
                )
        );

        checkInColumn.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getCheckInDate()
                )
        );

        checkOutColumn.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getCheckOutDate()
                )
        );

        statusColumn.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getStatus()
                )
        );

        reservationTable.setPlaceholder(
                new Label(
                        "No reservations match the selected filters."
                )
        );
    }

    private void configureStatusFilter() {
        statusFilter.setItems(
                FXCollections.observableArrayList(
                        ReservationStatus.values()
                )
        );

        statusFilter.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(
                            ReservationStatus status
                    ) {
                        return status == null
                                ? ""
                                : formatEnum(status.name());
                    }

                    @Override
                    public ReservationStatus fromString(
                            String value
                    ) {
                        return value == null || value.isBlank()
                                ? null
                                : ReservationStatus.valueOf(
                                value.toUpperCase()
                                        .replace(' ', '_')
                        );
                    }
                }
        );
    }

    @FXML
    private void searchReservations() {
        try {
            var reservations =
                    reservationManagementService
                            .searchReservations(
                                    searchField.getText(),
                                    statusFilter.getValue(),
                                    dateFromPicker.getValue(),
                                    dateToPicker.getValue()
                            );

            pagination.setItems(reservations);

        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        statusFilter.getSelectionModel().clearSelection();
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);

        searchReservations();
    }

    @FXML
    private void previousPage() {
        pagination.previousPage();
    }

    @FXML
    private void nextPage() {
        pagination.nextPage();
    }

    @FXML
    private void viewDetails() {
        Reservation selectedReservation =
                getSelectedReservation();

        if (selectedReservation == null) {
            return;
        }

        try {
            Reservation detailedReservation =
                    reservationManagementService
                            .findReservation(
                                    selectedReservation
                                            .getReservationId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Reservation was not found."
                                    )
                            );

            Alert alert =
                    new Alert(Alert.AlertType.INFORMATION);

            alert.setTitle("Reservation Details");
            alert.setHeaderText(
                    "Reservation RES-"
                            + detailedReservation
                            .getReservationId()
            );
            alert.setContentText(
                    buildReservationDetails(
                            detailedReservation
                    )
            );
            alert.getDialogPane().setPrefWidth(600);
            alert.showAndWait();

        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void createPhoneReservation() {
        openReservationForm(null);
    }

    @FXML
    private void modifyReservation() {
        Reservation selectedReservation =
                getSelectedReservation();

        if (selectedReservation == null) {
            return;
        }

        try {
            Reservation detailedReservation =
                    reservationManagementService
                            .findReservation(
                                    selectedReservation
                                            .getReservationId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Reservation was not found."
                                    )
                            );

            openReservationForm(detailedReservation);

        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    private void openReservationForm(
            Reservation reservationToEdit
    ) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/views/admin/"
                                    + "ReservationFormView.fxml"
                    )
            );

            loader.setControllerFactory(
                    AppConfig::createController
            );

            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.initOwner(
                    reservationTable
                            .getScene()
                            .getWindow()
            );
            dialogStage.initModality(
                    Modality.WINDOW_MODAL
            );
            dialogStage.setResizable(true);
            dialogStage.setScene(new Scene(root));

            ReservationFormController formController =
                    loader.getController();

            if (reservationToEdit == null) {
                dialogStage.setTitle(
                        "New Phone Reservation"
                );
                formController.configureForCreate(
                        dialogStage
                );
            } else {
                dialogStage.setTitle(
                        "Modify Reservation RES-"
                                + reservationToEdit
                                .getReservationId()
                );
                formController.configureForEdit(
                        dialogStage,
                        reservationToEdit
                );
            }

            Rectangle2D visualBounds =
                    Screen.getPrimary().getVisualBounds();
            double screenMargin = 60.0;
            double preferredWidth = 820.0;
            double preferredHeight = 760.0;
            dialogStage.setWidth(
                    Math.min(
                            preferredWidth,
                            visualBounds.getWidth()
                                    - screenMargin
                    )
            );
            dialogStage.setHeight(
                    Math.min(
                            preferredHeight,
                            visualBounds.getHeight()
                                    - screenMargin
                    )
            );
            dialogStage.centerOnScreen();

            dialogStage.showAndWait();

            if (!formController.isSaved()) {
                return;
            }

            Reservation savedReservation =
                    formController.getSavedReservation();

            searchReservations();

            showInformation(
                    reservationToEdit == null
                            ? "Reservation Created"
                            : "Reservation Updated",
                    "Reservation RES-"
                            + savedReservation
                            .getReservationId()
                            + (reservationToEdit == null
                            ? " was created successfully."
                            : " was updated successfully.")
            );

        } catch (IOException | RuntimeException exception) {
            showError(
                    "The reservation form could not be opened: "
                            + exception.getMessage()
            );
        }
    }

    @FXML
    private void cancelReservation() {
        Reservation selectedReservation =
                getSelectedReservation();

        if (selectedReservation == null) {
            return;
        }

        Alert confirmation =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Cancel Reservation");
        confirmation.setHeaderText(
                "Cancel reservation RES-"
                        + selectedReservation.getReservationId()
                        + "?"
        );
        confirmation.setContentText(
                "The reservation status will be changed to Cancelled."
        );

        ButtonType selectedButton =
                confirmation.showAndWait()
                        .orElse(ButtonType.CANCEL);

        if (selectedButton != ButtonType.OK) {
            return;
        }

        try {
            reservationManagementService.cancelReservation(
                    selectedReservation.getReservationId()
            );

            searchReservations();

            showInformation(
                    "Reservation Cancelled",
                    "Reservation RES-"
                            + selectedReservation.getReservationId()
                            + " is now cancelled."
            );

        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    private Reservation getSelectedReservation() {
        Reservation selectedReservation =
                reservationTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedReservation == null) {
            showError("Please select a reservation.");
        }

        return selectedReservation;
    }

    private String buildReservationDetails(
            Reservation reservation
    ) {
        return """
                Guest: %s
                Email: %s
                Phone: %s
                Address: %s

                Check-in: %s
                Check-out: %s
                Adults: %d
                Children: %d
                Group Booking: %s

                Rooms: %s
                Status: %s
                Created: %s
                """.formatted(
                reservation.getGuest().getFullName(),
                reservation.getGuest().getEmail(),
                reservation.getGuest().getPhone(),
                reservation.getGuest().getAddress(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getNumAdults(),
                reservation.getNumChildren(),
                reservation.isGroupBooking()
                        ? "Yes"
                        : "No",
                buildRoomPlan(reservation),
                formatEnum(
                        reservation.getStatus().name()
                ),
                reservation.getCreatedAt()
        );
    }

    private String buildRoomPlan(
            Reservation reservation
    ) {
        List<ReservationRoom> assignedRooms =
                reservation.getReservationRooms();

        if (assignedRooms != null
                && !assignedRooms.isEmpty()) {

            return assignedRooms.stream()
                    .map(ReservationRoom::getRoom)
                    .map(this::formatRoom)
                    .collect(Collectors.joining(", "));
        }

        return formatRoom(reservation.getRoom());
    }

    private String formatRoom(Room room) {
        if (room == null) {
            return "Not assigned";
        }

        return room.getRoomNumber()
                + " ("
                + formatEnum(
                room.getRoomType().name()
        )
                + ")";
    }

    private String formatEnum(String value) {
        String formatted =
                value.toLowerCase().replace('_', ' ');

        return Character.toUpperCase(
                formatted.charAt(0)
        ) + formatted.substring(1);
    }

    private void showInformation(
            String header,
            String message
    ) {
        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Reservation Management");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Reservation Management");
        alert.setHeaderText(
                "Unable to complete the request"
        );
        alert.setContentText(
                message == null || message.isBlank()
                        ? "An unexpected error occurred."
                        : message
        );
        alert.showAndWait();
    }
}
