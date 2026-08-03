package com.hotelreservation.controller.admin;

import com.hotelreservation.model.RoomType;
import com.hotelreservation.model.WaitlistEntry;
import com.hotelreservation.model.WaitlistStatus;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.service.WaitlistRequest;
import com.hotelreservation.service.WaitlistService;
import com.hotelreservation.util.AlertUtil;
import com.hotelreservation.util.TablePaginationSupport;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;

public class WaitlistController
        extends BaseAdminController {

    private final WaitlistService service;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<WaitlistStatus> statusFilter;
    @FXML
    private Label resultLabel;
    @FXML
    private Label pageLabel;
    @FXML
    private Button previousPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Button convertButton;
    @FXML
    private Button cancelButton;

    @FXML
    private TableView<WaitlistEntry> waitlistTable;
    @FXML
    private TableColumn<WaitlistEntry, String> idColumn;
    @FXML
    private TableColumn<WaitlistEntry, String> guestColumn;
    @FXML
    private TableColumn<WaitlistEntry, RoomType> roomTypeColumn;
    @FXML
    private TableColumn<WaitlistEntry, LocalDate> checkInColumn;
    @FXML
    private TableColumn<WaitlistEntry, LocalDate> checkOutColumn;
    @FXML
    private TableColumn<WaitlistEntry, String> occupancyColumn;
    @FXML
    private TableColumn<WaitlistEntry, WaitlistStatus> entryStatusColumn;

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
    private ComboBox<RoomType> roomTypeComboBox;
    @FXML
    private DatePicker checkInPicker;
    @FXML
    private DatePicker checkOutPicker;
    @FXML
    private Spinner<Integer> adultsSpinner;
    @FXML
    private Spinner<Integer> childrenSpinner;

    private TablePaginationSupport<WaitlistEntry> pagination;

    public WaitlistController(
            AuthenticationService authenticationService,
            AdminSession adminSession,
            WaitlistService service
    ) {
        super(authenticationService, adminSession);
        this.service = service;
    }

    @FXML
    private void initialize() {
        statusFilter.setItems(
                FXCollections.observableArrayList(
                        WaitlistStatus.values()
                )
        );
        roomTypeComboBox.setItems(
                FXCollections.observableArrayList(
                        RoomType.values()
                )
        );
        adultsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1,
                        20,
                        1
                )
        );
        childrenSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0,
                        20,
                        0
                )
        );
        checkInPicker.setValue(LocalDate.now().plusDays(1));
        checkOutPicker.setValue(LocalDate.now().plusDays(2));
        configureTable();
        waitlistTable.setPlaceholder(
                new Label("No waitlist entries match the filters.")
        );
        convertButton.disableProperty().bind(
                waitlistTable.getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );
        cancelButton.disableProperty().bind(
                waitlistTable.getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );
        pagination = new TablePaginationSupport<>(
                waitlistTable,
                resultLabel,
                pageLabel,
                previousPageButton,
                nextPageButton,
                10,
                "waitlist entry",
                "waitlist entries"
        );
        searchEntries();
    }

    private void configureTable() {
        idColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        "WAIT-"
                                + data.getValue().getWaitlistId()
                )
        );
        guestColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getGuest().getFullName()
                )
        );
        roomTypeColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getDesiredRoomType()
                )
        );
        checkInColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getCheckInDate()
                )
        );
        checkOutColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getCheckOutDate()
                )
        );
        occupancyColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getNumAdults()
                                + " adult(s), "
                                + data.getValue()
                                .getNumChildren()
                                + " child(ren)"
                )
        );
        entryStatusColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getStatus()
                )
        );
    }

    @FXML
    private void searchEntries() {
        try {
            var entries = service.search(
                    searchField.getText(),
                    statusFilter.getValue()
            );
            pagination.setItems(entries);
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Waitlist could not be loaded",
                    exception
            );
        }
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        statusFilter.setValue(null);
        searchEntries();
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
    private void addEntry() {
        try {
            WaitlistEntry entry = service.add(
                    new WaitlistRequest(
                            firstNameField.getText(),
                            lastNameField.getText(),
                            emailField.getText(),
                            phoneField.getText(),
                            addressArea.getText(),
                            roomTypeComboBox.getValue(),
                            checkInPicker.getValue(),
                            checkOutPicker.getValue(),
                            adultsSpinner.getValue(),
                            childrenSpinner.getValue()
                    )
            );
            AlertUtil.info(
                    "Waitlist",
                    "WAIT-" + entry.getWaitlistId()
                            + " was added."
            );
            resetForm();
            searchEntries();
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Waitlist entry could not be added",
                    exception
            );
        }
    }

    @FXML
    private void convertEntry() {
        try {
            WaitlistEntry entry = requireSelected();
            var reservation = service.convertToReservation(
                    entry.getWaitlistId()
            );
            AlertUtil.info(
                    "Waitlist Converted",
                    "Created RES-"
                            + reservation.getReservationId()
                            + "."
            );
            searchEntries();
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Waitlist conversion failed",
                    exception
            );
        }
    }

    @FXML
    private void cancelEntry() {
        try {
            WaitlistEntry entry = requireSelected();
            if (!AlertUtil.confirm(
                    "Cancel Waitlist Entry",
                    "Cancel WAIT-"
                            + entry.getWaitlistId() + "?"
            )) {
                return;
            }
            service.cancel(entry.getWaitlistId());
            searchEntries();
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Waitlist entry could not be cancelled",
                    exception
            );
        }
    }

    private WaitlistEntry requireSelected() {
        WaitlistEntry entry =
                waitlistTable.getSelectionModel()
                        .getSelectedItem();
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Select a waitlist entry first."
            );
        }
        return entry;
    }

    private void resetForm() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        addressArea.clear();
        roomTypeComboBox.setValue(null);
        adultsSpinner.getValueFactory().setValue(1);
        childrenSpinner.getValueFactory().setValue(0);
        checkInPicker.setValue(LocalDate.now().plusDays(1));
        checkOutPicker.setValue(LocalDate.now().plusDays(2));
    }
}
