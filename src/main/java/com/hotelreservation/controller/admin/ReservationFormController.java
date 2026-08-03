package com.hotelreservation.controller.admin;

import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.ReservationRoom;
import com.hotelreservation.model.Room;
import com.hotelreservation.service.ReservationManagementService;
import com.hotelreservation.service.ReservationRequest;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ReservationFormController {

    private final ReservationManagementService
            reservationManagementService;

    @FXML
    private Label formTitleLabel;

    @FXML
    private Label formSubtitleLabel;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextArea addressArea;

    @FXML
    private DatePicker checkInDatePicker;

    @FXML
    private DatePicker checkOutDatePicker;

    @FXML
    private Spinner<Integer> adultsSpinner;

    @FXML
    private Spinner<Integer> childrenSpinner;

    @FXML
    private ListView<Room> availableRoomsList;

    @FXML
    private Label availabilityLabel;

    @FXML
    private Label occupancySummaryLabel;

    @FXML
    private Button saveButton;

    private Stage dialogStage;
    private Long editingReservationId;
    private Reservation savedReservation;
    private boolean saved;
    private boolean configuring;

    public ReservationFormController(
            ReservationManagementService
                    reservationManagementService
    ) {
        this.reservationManagementService =
                reservationManagementService;
    }

    @FXML
    private void initialize() {
        adultsSpinner.setValueFactory(
                new SpinnerValueFactory
                        .IntegerSpinnerValueFactory(
                        1,
                        20,
                        1
                )
        );

        childrenSpinner.setValueFactory(
                new SpinnerValueFactory
                        .IntegerSpinnerValueFactory(
                        0,
                        20,
                        0
                )
        );

        availableRoomsList
                .getSelectionModel()
                .setSelectionMode(
                        SelectionMode.MULTIPLE
                );

        availableRoomsList.setCellFactory(
                listView -> new ListCell<>() {
                    @Override
                    protected void updateItem(
                            Room room,
                            boolean empty
                    ) {
                        super.updateItem(room, empty);

                        if (empty || room == null) {
                            setText(null);
                            return;
                        }

                        setText(
                                "Room "
                                        + room.getRoomNumber()
                                        + " | "
                                        + formatEnum(
                                        room.getRoomType()
                                                .name()
                                )
                                        + " | Capacity "
                                        + room.getMaxOccupancy()
                                        + " | $"
                                        + String.format(
                                        "%.2f/night",
                                        room.getBasePrice()
                                )
                        );
                    }
                }
        );

        checkInDatePicker.valueProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                datesChanged()
                );

        checkOutDatePicker.valueProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                datesChanged()
                );

        adultsSpinner.getValueFactory()
                .valueProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                updateOccupancySummary()
                );

        childrenSpinner.getValueFactory()
                .valueProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                updateOccupancySummary()
                );

        availableRoomsList
                .getSelectionModel()
                .getSelectedItems()
                .addListener(
                        (ListChangeListener<Room>) change ->
                                updateOccupancySummary()
                );
    }

    public void configureForCreate(Stage dialogStage) {
        this.dialogStage = dialogStage;
        this.editingReservationId = null;

        formTitleLabel.setText(
                "New Phone Reservation"
        );
        formSubtitleLabel.setText(
                "Enter the guest, stay, occupancy, and room details."
        );
        saveButton.setText("Create Reservation");

        configuring = true;
        checkInDatePicker.setValue(
                LocalDate.now().plusDays(1)
        );
        checkOutDatePicker.setValue(
                LocalDate.now().plusDays(2)
        );
        configuring = false;

        loadAvailableRooms();
    }

    public void configureForEdit(
            Stage dialogStage,
            Reservation reservation
    ) {
        this.dialogStage = dialogStage;
        this.editingReservationId =
                reservation.getReservationId();

        formTitleLabel.setText(
                "Modify Reservation RES-"
                        + reservation.getReservationId()
        );
        formSubtitleLabel.setText(
                "Update guest details, stay dates, occupancy, or rooms."
        );
        saveButton.setText("Save Changes");

        configuring = true;

        firstNameField.setText(
                safeText(
                        reservation.getGuest()
                                .getFirstName()
                )
        );
        lastNameField.setText(
                safeText(
                        reservation.getGuest()
                                .getLastName()
                )
        );
        emailField.setText(
                safeText(
                        reservation.getGuest().getEmail()
                )
        );
        phoneField.setText(
                safeText(
                        reservation.getGuest().getPhone()
                )
        );
        addressArea.setText(
                safeText(
                        reservation.getGuest().getAddress()
                )
        );

        checkInDatePicker.setValue(
                reservation.getCheckInDate()
        );
        checkOutDatePicker.setValue(
                reservation.getCheckOutDate()
        );
        adultsSpinner.getValueFactory().setValue(
                reservation.getNumAdults()
        );
        childrenSpinner.getValueFactory().setValue(
                reservation.getNumChildren()
        );

        configuring = false;

        loadAvailableRooms();
        selectRooms(
                getAssignedRoomIds(reservation)
        );
        updateOccupancySummary();
    }

    public boolean isSaved() {
        return saved;
    }

    public Reservation getSavedReservation() {
        return savedReservation;
    }

    @FXML
    private void refreshAvailableRooms() {
        loadAvailableRooms();
    }

    @FXML
    private void saveReservation() {
        try {
            int numAdults = readSpinnerValue(
                    adultsSpinner,
                    "Adults",
                    1,
                    20
            );

            int numChildren = readSpinnerValue(
                    childrenSpinner,
                    "Children",
                    0,
                    20
            );

            List<Long> roomIds =
                    availableRoomsList
                            .getSelectionModel()
                            .getSelectedItems()
                            .stream()
                            .map(Room::getRoomId)
                            .toList();

            ReservationRequest request =
                    new ReservationRequest(
                            firstNameField.getText(),
                            lastNameField.getText(),
                            emailField.getText(),
                            phoneField.getText(),
                            addressArea.getText(),
                            checkInDatePicker.getValue(),
                            checkOutDatePicker.getValue(),
                            numAdults,
                            numChildren,
                            roomIds
                    );

            if (editingReservationId == null) {
                savedReservation =
                        reservationManagementService
                                .createPhoneReservation(
                                        request
                                );
            } else {
                savedReservation =
                        reservationManagementService
                                .modifyReservation(
                                        editingReservationId,
                                        request
                                );
            }

            saved = true;
            closeDialog();

        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    private int readSpinnerValue(
            Spinner<Integer> spinner,
            String fieldName,
            int minimum,
            int maximum
    ) {
        String enteredValue =
                spinner.getEditor().getText().trim();

        try {
            int value = Integer.parseInt(enteredValue);

            if (value < minimum || value > maximum) {
                throw new IllegalArgumentException(
                        fieldName
                                + " must be between "
                                + minimum
                                + " and "
                                + maximum
                                + "."
                );
            }

            spinner.getValueFactory().setValue(value);
            return value;

        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    fieldName + " must be a whole number."
            );
        }
    }

    @FXML
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void datesChanged() {
        if (!configuring) {
            loadAvailableRooms();
        }
    }

    private void loadAvailableRooms() {
        Set<Long> previouslySelected =
                new LinkedHashSet<>(
                        availableRoomsList
                                .getSelectionModel()
                                .getSelectedItems()
                                .stream()
                                .map(Room::getRoomId)
                                .toList()
                );

        LocalDate checkInDate =
                checkInDatePicker.getValue();
        LocalDate checkOutDate =
                checkOutDatePicker.getValue();

        if (checkInDate == null
                || checkOutDate == null
                || !checkOutDate.isAfter(checkInDate)) {

            availableRoomsList.setItems(
                    FXCollections.observableArrayList()
            );
            availabilityLabel.setText(
                    "Choose a valid check-in and check-out date."
            );
            updateOccupancySummary();
            return;
        }

        try {
            List<Room> availableRooms =
                    reservationManagementService
                            .findAvailableRooms(
                                    checkInDate,
                                    checkOutDate,
                                    editingReservationId
                            );

            availableRoomsList.setItems(
                    FXCollections.observableArrayList(
                            availableRooms
                    )
            );

            selectRooms(previouslySelected);

            availabilityLabel.setText(
                    availableRooms.isEmpty()
                            ? "No rooms are available for these dates."
                            : availableRooms.size()
                            + " room(s) available. "
                            + "Use Ctrl+Click to select multiple rooms."
            );

        } catch (RuntimeException exception) {
            availableRoomsList.setItems(
                    FXCollections.observableArrayList()
            );
            availabilityLabel.setText(
                    exception.getMessage()
            );
        }

        updateOccupancySummary();
    }

    private void selectRooms(Set<Long> roomIds) {
        availableRoomsList
                .getSelectionModel()
                .clearSelection();

        for (int index = 0;
             index < availableRoomsList.getItems().size();
             index++) {

            Room room =
                    availableRoomsList.getItems().get(index);

            if (roomIds.contains(room.getRoomId())) {
                availableRoomsList
                        .getSelectionModel()
                        .select(index);
            }
        }
    }

    private Set<Long> getAssignedRoomIds(
            Reservation reservation
    ) {
        Set<Long> roomIds = new LinkedHashSet<>();

        List<ReservationRoom> reservationRooms =
                reservation.getReservationRooms();

        if (reservationRooms != null
                && !reservationRooms.isEmpty()) {

            for (ReservationRoom reservationRoom
                    : reservationRooms) {

                if (reservationRoom.getRoom() != null) {
                    roomIds.add(
                            reservationRoom
                                    .getRoom()
                                    .getRoomId()
                    );
                }
            }
        } else if (reservation.getRoom() != null) {
            roomIds.add(
                    reservation.getRoom().getRoomId()
            );
        }

        return roomIds;
    }

    private void updateOccupancySummary() {
        List<Room> selectedRooms =
                new ArrayList<>(
                        availableRoomsList
                                .getSelectionModel()
                                .getSelectedItems()
                );

        int selectedCapacity =
                selectedRooms.stream()
                        .mapToInt(Room::getMaxOccupancy)
                        .sum();

        int totalGuests =
                adultsSpinner.getValue()
                        + childrenSpinner.getValue();

        occupancySummaryLabel.setText(
                "Selected rooms: "
                        + selectedRooms.size()
                        + " | Capacity: "
                        + selectedCapacity
                        + " | Guests: "
                        + totalGuests
        );
    }

    private String formatEnum(String value) {
        String formatted =
                value.toLowerCase().replace('_', ' ');

        return Character.toUpperCase(
                formatted.charAt(0)
        ) + formatted.substring(1);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Reservation");
        alert.setHeaderText(
                "Unable to save the reservation"
        );
        alert.setContentText(
                message == null || message.isBlank()
                        ? "An unexpected error occurred."
                        : message
        );
        alert.showAndWait();
    }
}
