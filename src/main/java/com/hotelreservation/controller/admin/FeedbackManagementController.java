package com.hotelreservation.controller.admin;

import com.hotelreservation.model.Feedback;
import com.hotelreservation.model.SentimentTag;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.service.FeedbackService;
import com.hotelreservation.util.AlertUtil;
import com.hotelreservation.util.ExportUtil;
import com.hotelreservation.util.TablePaginationSupport;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class FeedbackManagementController
        extends BaseAdminController {

    private final FeedbackService service;

    @FXML
    private TextField guestFilter;
    @FXML
    private ComboBox<Integer> ratingFilter;
    @FXML
    private ComboBox<SentimentTag> sentimentFilter;
    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private Label summaryLabel;
    @FXML
    private Label paginationResultLabel;
    @FXML
    private Label pageLabel;
    @FXML
    private Button previousPageButton;
    @FXML
    private Button nextPageButton;

    @FXML
    private TableView<Feedback> feedbackTable;
    @FXML
    private TableColumn<Feedback, String> reservationColumn;
    @FXML
    private TableColumn<Feedback, String> guestColumn;
    @FXML
    private TableColumn<Feedback, Number> ratingColumn;
    @FXML
    private TableColumn<Feedback, String> commentColumn;
    @FXML
    private TableColumn<Feedback, SentimentTag> sentimentColumn;
    @FXML
    private TableColumn<Feedback, LocalDateTime> dateColumn;

    private TablePaginationSupport<Feedback> pagination;

    public FeedbackManagementController(
            AuthenticationService authenticationService,
            AdminSession adminSession,
            FeedbackService service
    ) {
        super(authenticationService, adminSession);
        this.service = service;
    }

    @FXML
    private void initialize() {
        ratingFilter.setItems(
                FXCollections.observableArrayList(
                        1, 2, 3, 4, 5
                )
        );
        sentimentFilter.setItems(
                FXCollections.observableArrayList(
                        SentimentTag.values()
                )
        );
        configureTable();
        feedbackTable.setPlaceholder(
                new Label("No feedback matches the selected filters.")
        );
        pagination = new TablePaginationSupport<>(
                feedbackTable,
                paginationResultLabel,
                pageLabel,
                previousPageButton,
                nextPageButton,
                10,
                "feedback entry",
                "feedback entries"
        );
        searchFeedback();
    }

    private void configureTable() {
        reservationColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        "RES-"
                                + data.getValue()
                                .getReservation()
                                .getReservationId()
                )
        );
        guestColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getGuest().getFullName()
                )
        );
        ratingColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(
                        data.getValue().getRating()
                )
        );
        commentColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getComment()
                )
        );
        sentimentColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getSentimentTag()
                )
        );
        dateColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().getSubmittedAt()
                )
        );
    }

    @FXML
    private void searchFeedback() {
        try {
            var feedback = service.searchAdminFeedback(
                    guestFilter.getText(),
                    ratingFilter.getValue(),
                    sentimentFilter.getValue(),
                    fromPicker.getValue(),
                    toPicker.getValue()
            );
            pagination.setItems(feedback);
            var summary = service.summarize(feedback);
            summaryLabel.setText(
                    String.format(
                            "%d feedback entries | Average: %.2f | "
                                    + "Issues: Cleanliness %d, "
                                    + "Service %d, Noise %d, "
                                    + "Billing %d, Needs review %d",
                            summary.feedbackCount(),
                            summary.averageRating(),
                            summary.count(
                                    SentimentTag.CLEANLINESS
                            ),
                            summary.count(SentimentTag.SERVICE),
                            summary.count(SentimentTag.NOISE),
                            summary.count(SentimentTag.BILLING),
                            summary.count(
                                    SentimentTag.NEEDS_REVIEW
                            )
                    )
            );
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Feedback could not be loaded",
                    exception
            );
        }
    }

    @FXML
    private void clearFilters() {
        guestFilter.clear();
        ratingFilter.setValue(null);
        sentimentFilter.setValue(null);
        fromPicker.setValue(null);
        toPicker.setValue(null);
        searchFeedback();
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
    private void exportCsv() {
        try {
            List<List<String>> rows =
                    pagination.getAllItems()
                            .stream()
                            .map(feedback -> List.of(
                                    "RES-"
                                            + feedback
                                            .getReservation()
                                            .getReservationId(),
                                    feedback.getGuest()
                                            .getFullName(),
                                    String.valueOf(
                                            feedback.getRating()
                                    ),
                                    feedback.getComment(),
                                    feedback.getSubmittedAt()
                                            .toString(),
                                    feedback.getSentimentTag()
                                            .name()
                            ))
                            .toList();

            Path path = ExportUtil.writeCsv(
                    "feedback-summary",
                    List.of(
                            "Reservation",
                            "Guest",
                            "Rating",
                            "Comment",
                            "Date",
                            "Sentiment"
                    ),
                    rows
            );
            AlertUtil.info(
                    "Feedback Exported",
                    path.toAbsolutePath().toString()
            );
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Feedback export failed",
                    exception
            );
        }
    }
}
