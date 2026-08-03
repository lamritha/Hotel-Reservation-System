package com.hotelreservation.controller.admin;

import com.hotelreservation.model.LoyaltyAccount;
import com.hotelreservation.model.LoyaltyTransaction;
import com.hotelreservation.model.LoyaltyTransactionType;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.service.LoyaltyService;
import com.hotelreservation.util.AlertUtil;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;

public class LoyaltyManagementController
        extends BaseAdminController {

    private final LoyaltyService service;

    @FXML
    private TextField searchField;
    @FXML
    private Label resultLabel;
    @FXML
    private Label selectedAccountLabel;

    @FXML
    private TableView<LoyaltyAccount> accountTable;
    @FXML
    private TableColumn<LoyaltyAccount, String> numberColumn;
    @FXML
    private TableColumn<LoyaltyAccount, String> guestColumn;
    @FXML
    private TableColumn<LoyaltyAccount, String> phoneColumn;
    @FXML
    private TableColumn<LoyaltyAccount, Number> balanceColumn;
    @FXML
    private TableColumn<LoyaltyAccount, Number> earnedColumn;
    @FXML
    private TableColumn<LoyaltyAccount, Number> redeemedColumn;

    @FXML
    private TableView<LoyaltyTransaction> historyTable;
    @FXML
    private TableColumn<LoyaltyTransaction, LocalDateTime> dateColumn;
    @FXML
    private TableColumn<LoyaltyTransaction, LoyaltyTransactionType> typeColumn;
    @FXML
    private TableColumn<LoyaltyTransaction, Number> pointsColumn;
    @FXML
    private TableColumn<LoyaltyTransaction, Number> monetaryColumn;
    @FXML
    private TableColumn<LoyaltyTransaction, String> descriptionColumn;

    public LoyaltyManagementController(
            AuthenticationService authenticationService,
            AdminSession adminSession,
            LoyaltyService service
    ) {
        super(authenticationService, adminSession);
        this.service = service;
    }

    @FXML
    private void initialize() {
        configureTables();
        accountTable.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, account) ->
                                displayAccount(account)
                );
        searchAccounts();
    }

    private void configureTables() {
        numberColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getLoyaltyNumber()
                )
        );
        guestColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getGuest().getFullName()
                )
        );
        phoneColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getGuest().getPhone()
                )
        );
        balanceColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(
                        data.getValue().getPointsBalance()
                )
        );
        earnedColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(
                        data.getValue().getTotalEarned()
                )
        );
        redeemedColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(
                        data.getValue().getTotalRedeemed()
                )
        );

        dateColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getCreatedAt()
                )
        );
        typeColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getTransactionType()
                )
        );
        pointsColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(
                        data.getValue().getPointsChange()
                )
        );
        monetaryColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().getMonetaryAmount()
                )
        );
        descriptionColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getDescription()
                )
        );
    }

    @FXML
    private void searchAccounts() {
        try {
            var accounts = service.searchAccounts(
                    searchField.getText()
            );
            accountTable.setItems(
                    FXCollections.observableArrayList(accounts)
            );
            resultLabel.setText(
                    accounts.size() + " account(s)"
            );
            if (!accounts.isEmpty()) {
                accountTable.getSelectionModel().selectFirst();
            }
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Loyalty accounts could not be loaded",
                    exception
            );
        }
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        searchAccounts();
    }

    private void displayAccount(LoyaltyAccount account) {
        if (account == null) {
            selectedAccountLabel.setText(
                    "Select a loyalty account."
            );
            historyTable.getItems().clear();
            return;
        }
        selectedAccountLabel.setText(
                account.getLoyaltyNumber()
                        + " | "
                        + account.getGuest().getFullName()
                        + " | Balance: "
                        + account.getPointsBalance()
                        + " points"
        );
        historyTable.setItems(
                FXCollections.observableArrayList(
                        service.getHistory(
                                account.getLoyaltyID()
                        )
                )
        );
    }
}
