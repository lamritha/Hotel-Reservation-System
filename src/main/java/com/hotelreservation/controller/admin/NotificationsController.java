package com.hotelreservation.controller.admin;

import com.hotelreservation.model.AdminNotification;
import com.hotelreservation.model.NotificationType;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.service.NotificationService;
import com.hotelreservation.util.AlertUtil;
import com.hotelreservation.util.TablePaginationSupport;
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

import java.time.LocalDateTime;

public class NotificationsController
        extends BaseAdminController {

    private final NotificationService service;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<NotificationType> typeFilter;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private Label resultLabel;
    @FXML
    private Label pageLabel;
    @FXML
    private Button previousPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Button markReadButton;
    @FXML
    private Button archiveButton;
    @FXML
    private TableView<AdminNotification> notificationTable;
    @FXML
    private TableColumn<AdminNotification, String> idColumn;
    @FXML
    private TableColumn<AdminNotification, NotificationType> typeColumn;
    @FXML
    private TableColumn<AdminNotification, String> messageColumn;
    @FXML
    private TableColumn<AdminNotification, LocalDateTime> createdColumn;
    @FXML
    private TableColumn<AdminNotification, String> statusColumn;

    private TablePaginationSupport<AdminNotification> pagination;

    public NotificationsController(
            AuthenticationService authenticationService,
            AdminSession adminSession,
            NotificationService service
    ) {
        super(authenticationService, adminSession);
        this.service = service;
    }

    @FXML
    private void initialize() {
        typeFilter.setItems(
                FXCollections.observableArrayList(
                        NotificationType.values()
                )
        );
        statusFilter.setItems(
                FXCollections.observableArrayList(
                        "ALL", "UNREAD", "READ"
                )
        );
        statusFilter.setValue("ALL");

        idColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        "NOT-"
                                + data.getValue()
                                .getNotificationId()
                )
        );
        typeColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getNotificationType()
                )
        );
        messageColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getMessage()
                )
        );
        createdColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getCreatedAt()
                )
        );
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().isRead()
                                ? "READ"
                                : "UNREAD"
                )
        );
        notificationTable.setPlaceholder(
                new Label("No notifications match the filters.")
        );
        markReadButton.disableProperty().bind(
                notificationTable.getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );
        archiveButton.disableProperty().bind(
                notificationTable.getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );
        pagination = new TablePaginationSupport<>(
                notificationTable,
                resultLabel,
                pageLabel,
                previousPageButton,
                nextPageButton,
                10,
                "notification",
                "notifications"
        );
        searchNotifications();
    }

    @FXML
    private void searchNotifications() {
        try {
            Boolean read = switch (statusFilter.getValue()) {
                case "READ" -> true;
                case "UNREAD" -> false;
                default -> null;
            };
            var notifications = service.search(
                    searchField.getText(),
                    typeFilter.getValue(),
                    read
            );
            pagination.setItems(notifications);
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Notifications could not be loaded",
                    exception
            );
        }
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        typeFilter.setValue(null);
        statusFilter.setValue("ALL");
        searchNotifications();
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
    private void markRead() {
        try {
            service.markRead(
                    notificationTable.getSelectionModel()
                            .getSelectedItem()
            );
            searchNotifications();
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Notification could not be updated",
                    exception
            );
        }
    }

    @FXML
    private void archive() {
        try {
            service.archive(
                    notificationTable.getSelectionModel()
                            .getSelectedItem()
            );
            searchNotifications();
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Notification could not be archived",
                    exception
            );
        }
    }
}
