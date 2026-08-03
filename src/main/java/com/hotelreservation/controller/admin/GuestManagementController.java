package com.hotelreservation.controller.admin;

import com.hotelreservation.model.Guest;
import com.hotelreservation.model.LoyaltyAccount;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.service.GuestManagementService;
import com.hotelreservation.util.AlertUtil;
import com.hotelreservation.util.TablePaginationSupport;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class GuestManagementController
        extends BaseAdminController {

    private final GuestManagementService service;

    @FXML
    private TextField searchField;
    @FXML
    private Label resultLabel;
    @FXML
    private Label selectedGuestLabel;
    @FXML
    private Label pageLabel;
    @FXML
    private Button previousPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Button enrollButton;
    @FXML
    private TableView<Guest> guestTable;
    @FXML
    private TableColumn<Guest, String> nameColumn;
    @FXML
    private TableColumn<Guest, String> emailColumn;
    @FXML
    private TableColumn<Guest, String> phoneColumn;
    @FXML
    private TableColumn<Guest, String> addressColumn;

    private TablePaginationSupport<Guest> pagination;

    public GuestManagementController(
            AuthenticationService authenticationService,
            AdminSession adminSession,
            GuestManagementService service
    ) {
        super(authenticationService, adminSession);
        this.service = service;
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getFullName()
                )
        );
        emailColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getEmail()
                )
        );
        phoneColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getPhone()
                )
        );
        addressColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getAddress()
                )
        );
        guestTable.setPlaceholder(
                new Label("No guests match the search.")
        );
        guestTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, guest) ->
                        selectedGuestLabel.setText(
                                guest == null
                                        ? "No guest selected"
                                        : guest.getFullName()
                                        + " | "
                                        + guest.getEmail()
                        )
                );
        enrollButton.disableProperty().bind(
                guestTable.getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );
        pagination = new TablePaginationSupport<>(
                guestTable,
                resultLabel,
                pageLabel,
                previousPageButton,
                nextPageButton,
                10,
                "guest",
                "guests"
        );
        searchGuests();
    }

    @FXML
    private void searchGuests() {
        try {
            var guests = service.search(
                    searchField.getText()
            );
            pagination.setItems(guests);
        } catch (RuntimeException exception) {
            AlertUtil.error("Guest search failed", exception);
        }
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        searchGuests();
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
    private void enrollSelectedGuest() {
        try {
            LoyaltyAccount account =
                    service.enrollInLoyalty(
                            guestTable.getSelectionModel()
                                    .getSelectedItem()
                    );
            AlertUtil.info(
                    "Loyalty Enrollment",
                    "Loyalty number: "
                            + account.getLoyaltyNumber()
            );
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Loyalty enrollment failed",
                    exception
            );
        }
    }
}
