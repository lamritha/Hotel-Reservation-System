package com.hotelreservation.controller.admin;

import com.hotelreservation.model.ReportPeriod;
import com.hotelreservation.model.RoomType;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.service.ReportService;
import com.hotelreservation.util.ActivityLogService;
import com.hotelreservation.util.AlertUtil;
import com.hotelreservation.util.ExportUtil;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReportsController
        extends BaseAdminController {

    private static final String REVENUE = "REVENUE";
    private static final String OCCUPANCY = "OCCUPANCY";
    private static final String ACTIVITY = "ACTIVITY LOGS";

    private final ReportService service;

    @FXML
    private ComboBox<String> reportTypeComboBox;
    @FXML
    private ComboBox<ReportPeriod> periodComboBox;
    @FXML
    private ComboBox<RoomType> roomTypeComboBox;
    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private Label statusLabel;

    @FXML
    private TableView<ReportService.RevenueReportRow>
            revenueTable;
    @FXML
    private TableColumn<ReportService.RevenueReportRow, String>
            revenuePeriodColumn;
    @FXML
    private TableColumn<ReportService.RevenueReportRow, Number>
            reservationCountColumn;
    @FXML
    private TableColumn<ReportService.RevenueReportRow, Number>
            subtotalColumn;
    @FXML
    private TableColumn<ReportService.RevenueReportRow, Number>
            taxColumn;
    @FXML
    private TableColumn<ReportService.RevenueReportRow, Number>
            discountsColumn;
    @FXML
    private TableColumn<ReportService.RevenueReportRow, Number>
            revenueTotalColumn;

    @FXML
    private TableView<ReportService.OccupancyReportRow>
            occupancyTable;
    @FXML
    private TableColumn<ReportService.OccupancyReportRow, String>
            occupancyDateColumn;
    @FXML
    private TableColumn<ReportService.OccupancyReportRow, Number>
            availableColumn;
    @FXML
    private TableColumn<ReportService.OccupancyReportRow, Number>
            occupiedColumn;
    @FXML
    private TableColumn<ReportService.OccupancyReportRow, Number>
            percentageColumn;

    @FXML
    private TableView<ActivityLogService.ActivityLogRow>
            activityTable;
    @FXML
    private TableColumn<ActivityLogService.ActivityLogRow, LocalDateTime>
            activityTimeColumn;
    @FXML
    private TableColumn<ActivityLogService.ActivityLogRow, String>
            activityActorColumn;
    @FXML
    private TableColumn<ActivityLogService.ActivityLogRow, String>
            activityActionColumn;
    @FXML
    private TableColumn<ActivityLogService.ActivityLogRow, String>
            activityEntityColumn;
    @FXML
    private TableColumn<ActivityLogService.ActivityLogRow, String>
            activityIdColumn;
    @FXML
    private TableColumn<ActivityLogService.ActivityLogRow, String>
            activityMessageColumn;

    private List<ReportService.RevenueReportRow>
            revenueRows = List.of();
    private List<ReportService.OccupancyReportRow>
            occupancyRows = List.of();
    private List<ActivityLogService.ActivityLogRow>
            activityRows = List.of();

    public ReportsController(
            AuthenticationService authenticationService,
            AdminSession adminSession,
            ReportService service
    ) {
        super(authenticationService, adminSession);
        this.service = service;
    }

    @FXML
    private void initialize() {
        reportTypeComboBox.setItems(
                FXCollections.observableArrayList(
                        REVENUE,
                        OCCUPANCY,
                        ACTIVITY
                )
        );
        reportTypeComboBox.setValue(REVENUE);
        periodComboBox.setItems(
                FXCollections.observableArrayList(
                        ReportPeriod.values()
                )
        );
        periodComboBox.setValue(ReportPeriod.DAILY);
        roomTypeComboBox.setItems(
                FXCollections.observableArrayList(
                        RoomType.values()
                )
        );
        fromPicker.setValue(LocalDate.now().minusDays(30));
        toPicker.setValue(LocalDate.now());
        configureTables();
        generateReport();
    }

    private void configureTables() {
        revenuePeriodColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().period()
                )
        );
        reservationCountColumn.setCellValueFactory(data ->
                new ReadOnlyLongWrapper(
                        data.getValue().reservationCount()
                )
        );
        subtotalColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().subtotal()
                )
        );
        taxColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().tax()
                )
        );
        discountsColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().discounts()
                )
        );
        revenueTotalColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue().total()
                )
        );

        occupancyDateColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().period()
                )
        );
        availableColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(
                        data.getValue().roomsAvailable()
                )
        );
        occupiedColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(
                        data.getValue().roomsOccupied()
                )
        );
        percentageColumn.setCellValueFactory(data ->
                new ReadOnlyDoubleWrapper(
                        data.getValue()
                                .occupancyPercentage()
                )
        );

        activityTimeColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().timestamp()
                )
        );
        activityActorColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().actor()
                )
        );
        activityActionColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().action()
                )
        );
        activityEntityColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().entityType()
                )
        );
        activityIdColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().entityIdentifier()
                )
        );
        activityMessageColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().message()
                )
        );
    }

    @FXML
    private void generateReport() {
        try {
            String type = reportTypeComboBox.getValue();
            if (REVENUE.equals(type)) {
                revenueRows = service.revenue(
                        fromPicker.getValue(),
                        toPicker.getValue(),
                        periodComboBox.getValue(),
                        roomTypeComboBox.getValue()
                );
                revenueTable.setItems(
                        FXCollections.observableArrayList(
                                revenueRows
                        )
                );
                showTable(revenueTable);
                statusLabel.setText(
                        revenueRows.size()
                                + " revenue row(s)"
                );
            } else if (OCCUPANCY.equals(type)) {
                occupancyRows = service.occupancy(
                        fromPicker.getValue(),
                        toPicker.getValue(),
                        periodComboBox.getValue(),
                        roomTypeComboBox.getValue()
                );
                occupancyTable.setItems(
                        FXCollections.observableArrayList(
                                occupancyRows
                        )
                );
                showTable(occupancyTable);
                statusLabel.setText(
                        occupancyRows.size()
                                + " occupancy row(s)"
                );
            } else {
                activityRows = service.activity();
                activityTable.setItems(
                        FXCollections.observableArrayList(
                                activityRows
                        )
                );
                showTable(activityTable);
                statusLabel.setText(
                        activityRows.size()
                                + " activity row(s)"
                );
            }
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Report generation failed",
                    exception
            );
        }
    }

    @FXML
    private void exportCsv() {
        export("CSV");
    }

    @FXML
    private void exportPdf() {
        export("PDF");
    }

    @FXML
    private void exportTxt() {
        export("TXT");
    }

    private void export(String format) {
        try {
            String type = reportTypeComboBox.getValue();
            List<String> headers;
            List<List<String>> rows;
            String baseName;

            if (REVENUE.equals(type)) {
                headers = List.of(
                        "Period",
                        "Reservations",
                        "Subtotal",
                        "Tax",
                        "Discounts",
                        "Total"
                );
                rows = revenueRows.stream()
                        .map(row -> List.of(
                                row.period(),
                                String.valueOf(
                                        row.reservationCount()
                                ),
                                money(row.subtotal()),
                                money(row.tax()),
                                money(row.discounts()),
                                money(row.total())
                        ))
                        .toList();
                baseName = "revenue-report";
            } else if (OCCUPANCY.equals(type)) {
                headers = List.of(
                        "Period",
                        "Rooms Available",
                        "Rooms Occupied",
                        "Occupancy Percentage"
                );
                rows = occupancyRows.stream()
                        .map(row -> List.of(
                                row.period(),
                                String.valueOf(
                                        row.roomsAvailable()
                                ),
                                String.valueOf(
                                        row.roomsOccupied()
                                ),
                                String.format(
                                        "%.2f",
                                        row.occupancyPercentage()
                                )
                        ))
                        .toList();
                baseName = "occupancy-report";
            } else {
                headers = List.of(
                        "Timestamp",
                        "Actor",
                        "Action",
                        "Entity Type",
                        "Entity ID",
                        "Message"
                );
                rows = activityRows.stream()
                        .map(row -> List.of(
                                row.timestamp().toString(),
                                row.actor(),
                                row.action(),
                                row.entityType(),
                                row.entityIdentifier(),
                                row.message()
                        ))
                        .toList();
                baseName = "activity-logs";
            }

            Path path = switch (format) {
                case "PDF" -> ExportUtil.writePdf(
                        baseName,
                        type + " REPORT",
                        headers,
                        rows
                );
                case "TXT" -> ExportUtil.writeTxt(
                        baseName,
                        headers,
                        rows
                );
                default -> ExportUtil.writeCsv(
                        baseName,
                        headers,
                        rows
                );
            };

            AlertUtil.info(
                    "Report Exported",
                    path.toAbsolutePath().toString()
            );
        } catch (RuntimeException exception) {
            AlertUtil.error(
                    "Report export failed",
                    exception
            );
        }
    }

    private void showTable(TableView<?> selected) {
        setVisible(revenueTable, selected == revenueTable);
        setVisible(occupancyTable, selected == occupancyTable);
        setVisible(activityTable, selected == activityTable);
    }

    private void setVisible(
            TableView<?> table,
            boolean visible
    ) {
        table.setVisible(visible);
        table.setManaged(visible);
    }

    private String money(double value) {
        return String.format("%.2f", value);
    }
}
