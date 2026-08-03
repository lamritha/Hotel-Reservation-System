package com.hotelreservation.controller.admin;

import com.hotelreservation.model.Billing;
import com.hotelreservation.model.Discount;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.service.DiscountService;
import com.hotelreservation.util.AlertUtil;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;

public class DiscountManagementController
        extends BaseAdminController {

    private final DiscountService service;

    @FXML
    private TextField searchField;
    @FXML
    private TextField percentageField;
    @FXML
    private TextField reasonField;
    @FXML
    private Label capLabel;
    @FXML
    private Label resultLabel;
    @FXML
    private TableView<Billing> billingTable;
    @FXML
    private TableColumn<Billing, String> reservationColumn;
    @FXML
    private TableColumn<Billing, String> guestColumn;
    @FXML
    private TableColumn<Billing, Number> totalColumn;
    @FXML
    private TableColumn<Billing, Number> percentColumn;
    @FXML
    private TableColumn<Billing, Number> amountColumn;
    @FXML
    private TableColumn<Billing, String> actorColumn;

    @FXML
    private TableView<Discount> historyTable;
    @FXML
    private TableColumn<Discount, LocalDateTime> historyDateColumn;
    @FXML
    private TableColumn<Discount, Number> historyPercentColumn;
    @FXML
    private TableColumn<Discount, Number> historyAmountColumn;
    @FXML
    private TableColumn<Discount, String> historyActorColumn;
    @FXML
    private TableColumn<Discount, String> historyReasonColumn;

    public DiscountManagementController(
            AuthenticationService authenticationService,
            AdminSession adminSession,
            DiscountService service
    ) {
        super(authenticationService, adminSession);
        this.service = service;
    }

    @FXML
    private void initialize() {
        capLabel.setText(
                String.format(
                        "Your role cap: %.0f%%",
                        service.getCurrentRoleCapPercent()
                )
        );
        configureTables();
        billingTable.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, billing) ->
                                showHistory(billing)
                );
        searchBills();
    }

    private void configureTables() {
        reservationColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        "RES-" + data.getValue()
                                .getReservation()
                                .getReservationId()
                )
        );
        guestColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getReservation()
                                .getGuest().getFullName()
                )
        );
        totalColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().getTotalAmount()
                )
        );
        percentColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue()
                                .getDiscountPercentage()
                                * 100
                )
        );
        amountColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().getDiscountAmount()
                )
        );
        actorColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue()
                                .getDiscountAppliedBy() == null
                                ? "-"
                                : data.getValue()
                                .getDiscountAppliedBy()
                )
        );

        historyDateColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getAppliedAt()
                )
        );
        historyPercentColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().getPercentage()
                )
        );
        historyAmountColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().getAmount()
                )
        );
        historyActorColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getAppliedBy()
                                + " ("
                                + data.getValue()
                                .getAppliedRole() + ")"
                )
        );
        historyReasonColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getReason()
                )
        );
    }

    @FXML
    private void searchBills() {
        try {
            var bills = service.searchBills(
                    searchField.getText()
            );
            billingTable.setItems(
                    FXCollections.observableArrayList(bills)
            );
            resultLabel.setText(
                    bills.size() + " bill(s)"
            );
            if (!bills.isEmpty()) {
                billingTable.getSelectionModel().selectFirst();
            }
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Discount records could not be loaded",
                    exception
            );
        }
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        searchBills();
    }

    @FXML
    private void applyDiscount() {
        try {
            Billing billing = billingTable
                    .getSelectionModel()
                    .getSelectedItem();
            if (billing == null) {
                throw new IllegalArgumentException(
                        "Select a billing record first."
                );
            }

            double percentage;
            try {
                percentage = Double.parseDouble(
                        percentageField.getText().trim()
                );
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Enter a valid discount percentage."
                );
            }

            service.applyDiscount(
                    billing.getReservation()
                            .getReservationId(),
                    percentage,
                    reasonField.getText()
            );
            AlertUtil.info(
                    "Discount",
                    "Discount applied successfully."
            );
            percentageField.clear();
            reasonField.clear();
            searchBills();
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Discount could not be applied",
                    exception
            );
        }
    }

    private void showHistory(Billing billing) {
        if (billing == null) {
            historyTable.getItems().clear();
            return;
        }
        historyTable.setItems(
                FXCollections.observableArrayList(
                        service.getHistory(
                                billing.getBillingId()
                        )
                )
        );
    }
}
