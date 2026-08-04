package com.hotelreservation.util;

import com.hotelreservation.config.AppConfig;
import com.hotelreservation.model.AdminRole;
import com.hotelreservation.model.Feedback;
import com.hotelreservation.model.PaymentMethod;
import com.hotelreservation.model.PaymentType;
import com.hotelreservation.model.ReportPeriod;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.ReservationStatus;
import com.hotelreservation.model.Room;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.service.BillingPaymentService;
import com.hotelreservation.service.FeedbackService;
import com.hotelreservation.service.ReportService;
import com.hotelreservation.service.ReservationManagementService;
import com.hotelreservation.service.ReservationRequest;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Triggers each export via ReportService / FeedbackService /
 * BillingPaymentService + ExportUtil (same data shape as admin UI)
 * and prints path, size, and CSV/TXT previews.
 */
public class VerifyExports {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        try {
            AppConfig.initialize();

            var login = AppConfig.getAuthenticationService()
                    .authenticate(
                            "admin",
                            "ChangeMe!2026Admin",
                            AdminRole.ADMIN
                    );
            if (!login.successful()) {
                System.out.println(
                        "VerifyExports failed: login — "
                                + login.message()
                );
                return;
            }

            ReportService reports = field("REPORT_SERVICE");
            FeedbackService feedback =
                    field("FEEDBACK_SERVICE");
            BillingPaymentService billing =
                    field("BILLING_PAYMENT_SERVICE");
            ReservationManagementService reservations =
                    AppConfig.getReservationManagementService();
            RoomRepository rooms = field("ROOM_REPOSITORY");

            Long checkedOutId = ensureCheckedOutWithFeedback(
                    reservations,
                    billing,
                    feedback,
                    rooms
            );

            LocalDate from = LocalDate.now().minusDays(30);
            LocalDate to = LocalDate.now().plusDays(30);

            System.out.println("=== REPORTS: REVENUE ===");
            List<ReportService.RevenueReportRow> revenue =
                    reports.revenue(
                            from, to, ReportPeriod.DAILY, null
                    );
            System.out.println("revenue_rows=" + revenue.size());
            List<String> revHeaders = List.of(
                    "Period",
                    "Reservations",
                    "Subtotal",
                    "Tax",
                    "Discounts",
                    "Total"
            );
            List<List<String>> revRows = revenue.stream()
                    .map(row -> List.of(
                            row.period(),
                            String.valueOf(row.reservationCount()),
                            money(row.subtotal()),
                            money(row.tax()),
                            money(row.discounts()),
                            money(row.total())
                    ))
                    .toList();
            reportFile(
                    "revenue CSV",
                    ExportUtil.writeCsv(
                            "revenue-report",
                            revHeaders,
                            revRows
                    ),
                    true
            );
            reportFile(
                    "revenue PDF",
                    ExportUtil.writePdf(
                            "revenue-report",
                            "Revenue REPORT",
                            revHeaders,
                            revRows
                    ),
                    false
            );

            System.out.println("=== REPORTS: OCCUPANCY ===");
            List<ReportService.OccupancyReportRow> occupancy =
                    reports.occupancy(
                            from, to, ReportPeriod.DAILY, null
                    );
            System.out.println(
                    "occupancy_rows=" + occupancy.size()
            );
            List<String> occHeaders = List.of(
                    "Period",
                    "Rooms Available",
                    "Rooms Occupied",
                    "Occupancy Percentage"
            );
            List<List<String>> occRows = occupancy.stream()
                    .map(row -> List.of(
                            row.period(),
                            String.valueOf(row.roomsAvailable()),
                            String.valueOf(row.roomsOccupied()),
                            String.format(
                                    "%.2f",
                                    row.occupancyPercentage()
                            )
                    ))
                    .toList();
            reportFile(
                    "occupancy CSV",
                    ExportUtil.writeCsv(
                            "occupancy-report",
                            occHeaders,
                            occRows
                    ),
                    true
            );
            reportFile(
                    "occupancy PDF",
                    ExportUtil.writePdf(
                            "occupancy-report",
                            "Occupancy REPORT",
                            occHeaders,
                            occRows
                    ),
                    false
            );

            System.out.println("=== REPORTS: ACTIVITY LOG ===");
            List<ActivityLogService.ActivityLogRow> activity =
                    reports.activity();
            System.out.println(
                    "activity_rows=" + activity.size()
            );
            List<String> actHeaders = List.of(
                    "Timestamp",
                    "Actor",
                    "Action",
                    "Entity Type",
                    "Entity ID",
                    "Message"
            );
            List<List<String>> actRows = activity.stream()
                    .map(row -> List.of(
                            row.timestamp().toString(),
                            row.actor(),
                            row.action(),
                            row.entityType(),
                            row.entityIdentifier(),
                            row.message()
                    ))
                    .toList();
            reportFile(
                    "activity CSV",
                    ExportUtil.writeCsv(
                            "activity-logs",
                            actHeaders,
                            actRows
                    ),
                    true
            );
            reportFile(
                    "activity TXT",
                    ExportUtil.writeTxt(
                            "activity-logs",
                            actHeaders,
                            actRows
                    ),
                    true
            );

            System.out.println("=== FEEDBACK CSV ===");
            List<Feedback> feedbackRows =
                    feedback.searchAdminFeedback(
                            null, null, null, null, null
                    );
            System.out.println(
                    "feedback_rows=" + feedbackRows.size()
            );
            List<List<String>> fbRows = feedbackRows.stream()
                    .map(entry -> List.of(
                            "RES-"
                                    + entry.getReservation()
                                    .getReservationId(),
                            entry.getGuest().getFullName(),
                            String.valueOf(entry.getRating()),
                            entry.getComment(),
                            entry.getSubmittedAt().toString(),
                            entry.getSentimentTag().name()
                    ))
                    .toList();
            reportFile(
                    "feedback CSV",
                    ExportUtil.writeCsv(
                            "feedback-summary",
                            List.of(
                                    "Reservation",
                                    "Guest",
                                    "Rating",
                                    "Comment",
                                    "Date",
                                    "Sentiment"
                            ),
                            fbRows
                    ),
                    true
            );

            System.out.println("=== FINAL BILL PDF ===");
            Path bill = billing.exportFinalBillPdf(checkedOutId);
            reportFile("final bill PDF", bill, false);

            System.out.println(
                    "=== SUMMARY passed=" + passed
                            + " failed=" + failed
                            + " ALL_PASS="
                            + (failed == 0)
                            + " ==="
            );
        } catch (Exception e) {
            System.out.println("VerifyExports failed.");
            e.printStackTrace();
        } finally {
            AppConfig.shutdown();
        }
    }

    private static Long ensureCheckedOutWithFeedback(
            ReservationManagementService reservations,
            BillingPaymentService billing,
            FeedbackService feedback,
            RoomRepository rooms
    ) throws Exception {
        LocalDate checkIn = LocalDate.of(2027, 6, 1);
        LocalDate checkOut = LocalDate.of(2027, 6, 3);
        long stamp = System.currentTimeMillis();

        Room room = rooms.findAvailableRoomsByTypeAndDates(
                        null,
                        checkIn,
                        checkOut,
                        1
                )
                .getFirst();

        Reservation stay = reservations.createPhoneReservation(
                new ReservationRequest(
                        "Export",
                        "Verify",
                        "export.verify" + stamp + "@email.com",
                        String.format(
                                "416-555-%04d",
                                Math.floorMod(stamp, 10000)
                        ),
                        "1 Export Verify St",
                        checkIn,
                        checkOut,
                        1,
                        0,
                        List.of(room.getRoomId())
                )
        );
        Long stayId = stay.getReservationId();
        System.out.println(
                "seed_reservation_id=" + stayId
                        + " room=" + room.getRoomNumber()
        );

        BillingPaymentService.BillingSummary summary =
                billing.getSummary(stayId);
        if (summary.outstanding() > 0.009) {
            billing.processPayment(
                    stayId,
                    summary.outstanding(),
                    PaymentMethod.CARD,
                    PaymentType.FINAL_PAYMENT,
                    "Settle for export verify"
            );
        }
        billing.checkIn(stayId);
        stay = billing.checkout(stayId);
        if (stay.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new IllegalStateException(
                    "Expected CHECKED_OUT, got "
                            + stay.getStatus()
            );
        }

        List<Feedback> existing =
                feedback.searchAdminFeedback(
                        null, null, null, null, null
                );
        if (existing.isEmpty()) {
            feedback.submit(
                    stayId,
                    5,
                    "Great staff service during export verify."
            );
            System.out.println("seed_feedback=submitted");
        } else {
            System.out.println(
                    "seed_feedback=already_have_"
                            + existing.size()
            );
        }
        return stayId;
    }

    private static void reportFile(
            String label,
            Path path,
            boolean previewText
    ) throws Exception {
        boolean exists = Files.isRegularFile(path);
        long size = exists ? Files.size(path) : -1;
        boolean ok = exists && size > 0;
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.println(
                (ok ? "PASS" : "FAIL")
                        + " " + label
                        + " path=" + path.toAbsolutePath()
                        + " bytes=" + size
        );
        if (previewText && exists) {
            List<String> lines = Files.readAllLines(
                    path,
                    StandardCharsets.UTF_8
            );
            int show = Math.min(5, lines.size());
            System.out.println(
                    "--- preview first " + show
                            + " line(s) ---"
            );
            for (int i = 0; i < show; i++) {
                System.out.println(lines.get(i));
            }
            if (lines.size() > show) {
                System.out.println(
                        "... (" + (lines.size() - show)
                                + " more line(s))"
                );
            }
        }
    }

    private static String money(double value) {
        return String.format("%.2f", value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(String name) throws Exception {
        Field f = AppConfig.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(null);
    }
}
