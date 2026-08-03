package com.hotelreservation.controller.admin;

import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomStatus;
import com.hotelreservation.model.RoomType;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.service.RoomManagementService;
import com.hotelreservation.util.AlertUtil;
import com.hotelreservation.util.TablePaginationSupport;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class RoomManagementController
        extends BaseAdminController {

    private final RoomManagementService service;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<RoomType> typeFilter;
    @FXML
    private ComboBox<RoomStatus> statusFilter;
    @FXML
    private Label resultLabel;
    @FXML
    private Label pageLabel;
    @FXML
    private Button previousPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Button availableButton;
    @FXML
    private Button occupiedButton;
    @FXML
    private Button maintenanceButton;
    @FXML
    private TableView<Room> roomTable;
    @FXML
    private TableColumn<Room, String> numberColumn;
    @FXML
    private TableColumn<Room, Number> floorColumn;
    @FXML
    private TableColumn<Room, RoomType> typeColumn;
    @FXML
    private TableColumn<Room, Number> priceColumn;
    @FXML
    private TableColumn<Room, Number> capacityColumn;
    @FXML
    private TableColumn<Room, RoomStatus> statusColumn;

    private TablePaginationSupport<Room> pagination;

    public RoomManagementController(
            AuthenticationService authenticationService,
            AdminSession adminSession,
            RoomManagementService service
    ) {
        super(authenticationService, adminSession);
        this.service = service;
    }

    @FXML
    private void initialize() {
        typeFilter.setItems(
                FXCollections.observableArrayList(
                        RoomType.values()
                )
        );
        statusFilter.setItems(
                FXCollections.observableArrayList(
                        RoomStatus.values()
                )
        );
        numberColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getRoomNumber()
                )
        );
        floorColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(
                        data.getValue().getFloor()
                )
        );
        typeColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getRoomType()
                )
        );
        priceColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().getBasePrice()
                )
        );
        capacityColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(
                        data.getValue().getMaxOccupancy()
                )
        );
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getStatus()
                )
        );
        roomTable.setPlaceholder(
                new Label("No rooms match the selected filters.")
        );
        availableButton.disableProperty().bind(
                roomTable.getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );
        occupiedButton.disableProperty().bind(
                roomTable.getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );
        maintenanceButton.disableProperty().bind(
                roomTable.getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );
        pagination = new TablePaginationSupport<>(
                roomTable,
                resultLabel,
                pageLabel,
                previousPageButton,
                nextPageButton,
                12,
                "room",
                "rooms"
        );
        searchRooms();
    }

    @FXML
    private void searchRooms() {
        try {
            var rooms = service.search(
                    searchField.getText(),
                    typeFilter.getValue(),
                    statusFilter.getValue()
            );
            pagination.setItems(rooms);
        } catch (RuntimeException exception) {
            AlertUtil.error("Room search failed", exception);
        }
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        typeFilter.setValue(null);
        statusFilter.setValue(null);
        searchRooms();
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
    private void markAvailable() {
        changeStatus(RoomStatus.AVAILABLE);
    }

    @FXML
    private void markOccupied() {
        changeStatus(RoomStatus.OCCUPIED);
    }

    @FXML
    private void markMaintenance() {
        changeStatus(RoomStatus.MAINTENANCE);
    }

    private void changeStatus(RoomStatus status) {
        try {
            service.changeStatus(
                    roomTable.getSelectionModel()
                            .getSelectedItem(),
                    status
            );
            searchRooms();
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Room status could not be changed",
                    exception
            );
        }
    }
}
