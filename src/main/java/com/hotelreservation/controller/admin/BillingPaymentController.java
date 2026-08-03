package com.hotelreservation.controller.admin;

import com.hotelreservation.model.Payment;
import com.hotelreservation.model.PaymentMethod;
import com.hotelreservation.model.PaymentType;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.service.BillingPaymentService;
import com.hotelreservation.util.AlertUtil;
import com.hotelreservation.util.ExportUtil;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BillingPaymentController
        extends BaseAdminController {

    private final BillingPaymentService service;

    @FXML
    private TextField searchField;
    @FXML
    private Label resultLabel;
    @FXML
    private TableView<BillingPaymentService.BillingSummary>
            billingTable;
    @FXML
    private TableColumn<BillingPaymentService.BillingSummary, String>
            reservationColumn;
    @FXML
    private TableColumn<BillingPaymentService.BillingSummary, String>
            guestColumn;
    @FXML
    private TableColumn<BillingPaymentService.BillingSummary, String>
            reservationStatusColumn;
    @FXML
    private TableColumn<BillingPaymentService.BillingSummary, Number>
            totalColumn;
    @FXML
    private TableColumn<BillingPaymentService.BillingSummary, Number>
            discountColumn;
    @FXML
    private TableColumn<BillingPaymentService.BillingSummary, Number>
            paidColumn;
    @FXML
    private TableColumn<BillingPaymentService.BillingSummary, Number>
            outstandingColumn;

    @FXML
    private Label selectedBillLabel;
    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<PaymentMethod> methodComboBox;
    @FXML
    private ComboBox<PaymentType> paymentTypeComboBox;
    @FXML
    private TextField noteField;

    @FXML
    private TableView<Payment> paymentTable;
    @FXML
    private TableColumn<Payment, LocalDateTime> paymentDateColumn;
    @FXML
    private TableColumn<Payment, PaymentMethod> paymentMethodColumn;
    @FXML
    private TableColumn<Payment, PaymentType> paymentTypeColumn;
    @FXML
    private TableColumn<Payment, Number> paymentAmountColumn;
    @FXML
    private TableColumn<Payment, String> paymentNoteColumn;

    public BillingPaymentController(
            AuthenticationService authenticationService,
            AdminSession adminSession,
            BillingPaymentService service
    ) {
        super(authenticationService, adminSession);
        this.service = service;
    }

    @FXML
    private void initialize() {
        configureBillingTable();
        configurePaymentTable();
        methodComboBox.setItems(
                FXCollections.observableArrayList(
                        PaymentMethod.values()
                )
        );
        methodComboBox.setValue(PaymentMethod.CARD);
        paymentTypeComboBox.setItems(
                FXCollections.observableArrayList(
                        PaymentType.DEPOSIT,
                        PaymentType.PARTIAL_PAYMENT,
                        PaymentType.FINAL_PAYMENT
                )
        );
        paymentTypeComboBox.setValue(
                PaymentType.PARTIAL_PAYMENT
        );

        billingTable.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, value) ->
                                displaySelectedBill(value)
                );
        searchBills();
    }

    private void configureBillingTable() {
        reservationColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        "RES-" + data.getValue()
                                .billing()
                                .getReservation()
                                .getReservationId()
                )
        );
        guestColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue()
                                .billing()
                                .getReservation()
                                .getGuest()
                                .getFullName()
                )
        );
        reservationStatusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue()
                                .billing()
                                .getReservation()
                                .getStatus()
                                .name()
                )
        );
        totalColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue()
                                .billing()
                                .getTotalAmount()
                )
        );
        discountColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue()
                                .billing()
                                .getDiscountAmount()
                )
        );
        paidColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().paid()
                )
        );
        outstandingColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().outstanding()
                )
        );
    }

    private void configurePaymentTable() {
        paymentDateColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getPaymentDate()
                )
        );
        paymentMethodColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getPaymentMethod()
                )
        );
        paymentTypeColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getPaymentType()
                )
        );
        paymentAmountColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().getAmount()
                )
        );
        paymentNoteColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getNote()
                )
        );
    }

    @FXML
    private void searchBills() {
        try {
            var summaries = service.search(
                    searchField.getText()
            );
            billingTable.setItems(
                    FXCollections.observableArrayList(
                            summaries
                    )
            );
            resultLabel.setText(
                    summaries.size() + " bill(s)"
            );
            if (!summaries.isEmpty()) {
                billingTable.getSelectionModel().selectFirst();
            } else {
                displaySelectedBill(null);
            }
        } catch (RuntimeException exception) {
            AlertUtil.error("Billing search failed", exception);
        }
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        searchBills();
    }

    @FXML
    private void processPayment() {
        try {
            BillingPaymentService.BillingSummary summary =
                    requireSelected();
            service.processPayment(
                    reservationId(summary),
                    parseAmount(),
                    methodComboBox.getValue(),
                    paymentTypeComboBox.getValue(),
                    noteField.getText()
            );
            AlertUtil.info(
                    "Payment",
                    "Payment processed successfully."
            );
            refreshReservation(reservationId(summary));
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Payment could not be processed",
                    exception
            );
        }
    }

    @FXML
    private void processRefund() {
        try {
            BillingPaymentService.BillingSummary summary =
                    requireSelected();
            if (!AlertUtil.confirm(
                    "Confirm Refund",
                    "Process this refund?"
            )) {
                return;
            }
            service.refund(
                    reservationId(summary),
                    parseAmount(),
                    methodComboBox.getValue(),
                    noteField.getText()
            );
            AlertUtil.info(
                    "Refund",
                    "Refund processed successfully."
            );
            refreshReservation(reservationId(summary));
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Refund could not be processed",
                    exception
            );
        }
    }

    @FXML
    private void checkIn() {
        changeStayStatus(true);
    }

    @FXML
    private void checkOut() {
        changeStayStatus(false);
    }

    private void changeStayStatus(boolean checkingIn) {
        try {
            BillingPaymentService.BillingSummary summary =
                    requireSelected();
            Long reservationId = reservationId(summary);
            if (checkingIn) {
                service.checkIn(reservationId);
            } else {
                service.checkout(reservationId);
            }
            AlertUtil.info(
                    checkingIn ? "Check In" : "Check Out",
                    checkingIn
                            ? "Guest checked in successfully."
                            : "Checkout completed. Invite the guest "
                            + "to submit feedback at the kiosk."
            );
            refreshReservation(reservationId);
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    checkingIn
                            ? "Check-in failed"
                            : "Checkout failed",
                    exception
            );
        }
    }

    @FXML
    private void exportFinalBill() {
        try {
            BillingPaymentService.BillingSummary summary =
                    requireSelected();
            var billing = summary.billing();
            List<List<String>> rows = new ArrayList<>();
            rows.add(List.of(
                    "Reservation",
                    "RES-" + reservationId(summary)
            ));
            rows.add(List.of(
                    "Guest",
                    billing.getReservation()
                            .getGuest()
                            .getFullName()
            ));
            rows.add(List.of(
                    "Subtotal",
                    money(billing.getSubtotal())
            ));
            rows.add(List.of(
                    "Tax",
                    money(billing.getTaxAmount())
            ));
            rows.add(List.of(
                    "Discount",
                    money(billing.getDiscountAmount())
            ));
            rows.add(List.of(
                    "Paid",
                    money(summary.paid())
            ));
            rows.add(List.of(
                    "Outstanding",
                    money(summary.outstanding())
            ));

            Path path = ExportUtil.writePdf(
                    "final-bill-RES-"
                            + reservationId(summary),
                    "Hotel Final Bill",
                    List.of("Item", "Amount / Value"),
                    rows
            );
            AlertUtil.info(
                    "Final Bill Exported",
                    path.toAbsolutePath().toString()
            );
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Final bill export failed",
                    exception
            );
        }
    }

    private void displaySelectedBill(
            BillingPaymentService.BillingSummary summary
    ) {
        if (summary == null) {
            selectedBillLabel.setText(
                    "Select a billing record."
            );
            paymentTable.getItems().clear();
            return;
        }

        selectedBillLabel.setText(
                "RES-" + reservationId(summary)
                        + " | "
                        + summary.billing()
                        .getReservation()
                        .getGuest()
                        .getFullName()
                        + " | Outstanding: "
                        + money(summary.outstanding())
        );
        paymentTable.setItems(
                FXCollections.observableArrayList(
                        service.getPayments(
                                summary.billing()
                                        .getBillingId()
                        )
                )
        );
    }

    private void refreshReservation(Long reservationId) {
        var summaries = service.search("RES-" + reservationId);
        billingTable.setItems(
                FXCollections.observableArrayList(summaries)
        );
        if (!summaries.isEmpty()) {
            billingTable.getSelectionModel().selectFirst();
        }
        amountField.clear();
        noteField.clear();
    }

    private BillingPaymentService.BillingSummary
    requireSelected() {
        BillingPaymentService.BillingSummary summary =
                billingTable.getSelectionModel()
                        .getSelectedItem();
        if (summary == null) {
            throw new IllegalArgumentException(
                    "Select a billing record first."
            );
        }
        return summary;
    }

    private double parseAmount() {
        try {
            return Double.parseDouble(
                    amountField.getText().trim()
            );
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Enter a valid payment amount."
            );
        }
    }

    private Long reservationId(
            BillingPaymentService.BillingSummary summary
    ) {
        return summary.billing()
                .getReservation()
                .getReservationId();
    }

    private String money(double value) {
        return String.format("CAD %.2f", value);
    }
}
