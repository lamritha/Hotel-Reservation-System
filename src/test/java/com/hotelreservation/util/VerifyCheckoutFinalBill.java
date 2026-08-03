package com.hotelreservation.util;

import com.hotelreservation.config.AppConfig;
import com.hotelreservation.model.AdminRole;
import com.hotelreservation.model.PaymentMethod;
import com.hotelreservation.model.PaymentType;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.ReservationStatus;
import com.hotelreservation.model.Room;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.service.BillingPaymentService;
import com.hotelreservation.service.ReservationManagementService;
import com.hotelreservation.service.ReservationRequest;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Verifies checkout auto-exports final bill PDF and manual export is
 * gated to CHECKED_OUT reservations.
 */
public class VerifyCheckoutFinalBill {

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
                        "VerifyCheckoutFinalBill failed: login — "
                                + login.message()
                );
                return;
            }

            ReservationManagementService reservations =
                    AppConfig.getReservationManagementService();
            BillingPaymentService billing =
                    billingPaymentService();
            RoomRepository rooms = roomRepository();

            LocalDate checkIn = LocalDate.of(2027, 3, 1);
            LocalDate checkOut = LocalDate.of(2027, 3, 3);
            long stamp = System.currentTimeMillis();

            Room room = rooms
                    .findAvailableRoomsByTypeAndDates(
                            null,
                            checkIn,
                            checkOut,
                            1
                    )
                    .getFirst();

            System.out.println(
                    "=== 1) CHECKOUT WITH ZERO BALANCE ==="
            );

            Reservation stay = reservations
                    .createPhoneReservation(
                            new ReservationRequest(
                                    "Checkout",
                                    "PdfTest",
                                    "checkout.pdf"
                                            + stamp
                                            + "@email.com",
                                    phone(stamp),
                                    "1 Checkout Pdf St",
                                    checkIn,
                                    checkOut,
                                    1,
                                    0,
                                    List.of(room.getRoomId())
                            )
                    );
            Long stayId = stay.getReservationId();
            System.out.println(
                    "created_reservation_id=" + stayId
                            + " status=" + stay.getStatus()
            );

            BillingPaymentService.BillingSummary beforePay =
                    billing.getSummary(stayId);
            double outstanding = beforePay.outstanding();
            System.out.println(
                    "outstanding_before_pay="
                            + outstanding
            );

            if (outstanding > 0.009) {
                billing.processPayment(
                        stayId,
                        outstanding,
                        PaymentMethod.CARD,
                        PaymentType.FINAL_PAYMENT,
                        "Settle before checkout verify"
                );
            }

            BillingPaymentService.BillingSummary afterPay =
                    billing.getSummary(stayId);
            System.out.println(
                    "outstanding_after_pay="
                            + afterPay.outstanding()
            );

            stay = billing.checkIn(stayId);
            System.out.println(
                    "after_check_in_status="
                            + stay.getStatus()
            );

            Path exportsDir = Path.of("exports");
            Files.createDirectories(exportsDir);
            long exportCountBefore = Files.list(exportsDir)
                    .filter(p -> p.getFileName()
                            .toString()
                            .startsWith(
                                    "final-bill-RES-"
                                            + stayId
                            ))
                    .count();

            stay = billing.checkout(stayId);
            System.out.println(
                    "after_checkout_status="
                            + stay.getStatus()
            );

            List<Path> newPdfs = Files.list(exportsDir)
                    .filter(p -> p.getFileName()
                            .toString()
                            .startsWith(
                                    "final-bill-RES-"
                                            + stayId
                            ))
                    .sorted()
                    .toList();
            long exportCountAfter = newPdfs.size();
            Path latestPdf = newPdfs.isEmpty()
                    ? null
                    : newPdfs.getLast();

            System.out.println(
                    "pdf_count_before=" + exportCountBefore
            );
            System.out.println(
                    "pdf_count_after=" + exportCountAfter
            );
            System.out.println(
                    "pdf_exists="
                            + (latestPdf != null
                            && Files.isRegularFile(latestPdf))
            );
            if (latestPdf != null) {
                System.out.println(
                        "pdf_path="
                                + latestPdf.toAbsolutePath()
                );
                System.out.println(
                        "pdf_bytes="
                                + Files.size(latestPdf)
                );
            }
            System.out.println(
                    "ui_success_message_would_include_pdf_text="
                            + "Checkout completed. Final bill PDF was generated. ..."
            );
            System.out.println(
                    "STEP1_PASS="
                            + (stay.getStatus()
                            == ReservationStatus.CHECKED_OUT
                            && latestPdf != null
                            && Files.isRegularFile(latestPdf)
                            && Files.size(latestPdf) > 0
                            && exportCountAfter
                            > exportCountBefore)
            );

            System.out.println(
                    "=== 2) MANUAL EXPORT WHILE NOT CHECKED_OUT ==="
            );
            Room room2 = rooms
                    .findAvailableRoomsByTypeAndDates(
                            null,
                            LocalDate.of(2027, 4, 1),
                            LocalDate.of(2027, 4, 3),
                            1
                    )
                    .getFirst();
            Reservation open = reservations
                    .createPhoneReservation(
                            new ReservationRequest(
                                    "Open",
                                    "ExportBlock",
                                    "open.export"
                                            + stamp
                                            + "@email.com",
                                    phone(stamp + 1),
                                    "2 Open Export St",
                                    LocalDate.of(2027, 4, 1),
                                    LocalDate.of(2027, 4, 3),
                                    1,
                                    0,
                                    List.of(
                                            room2.getRoomId()
                                    )
                            )
                    );
            System.out.println(
                    "open_reservation_id="
                            + open.getReservationId()
                            + " status="
                            + open.getStatus()
            );

            String blockedMessage = null;
            try {
                billing.exportFinalBillPdf(
                        open.getReservationId()
                );
                System.out.println(
                        "UNEXPECTED_EXPORT_SUCCEEDED"
                );
            } catch (RuntimeException exception) {
                blockedMessage = exception.getMessage();
                System.out.println(
                        "blocked_message=" + blockedMessage
                );
            }
            boolean blockedOk = blockedMessage != null
                    && blockedMessage.contains(
                    "Final bill can only be exported after checkout."
            );
            System.out.println("STEP2_PASS=" + blockedOk);

            System.out.println(
                    "=== 3) MANUAL RE-EXPORT AFTER CHECKOUT ==="
            );
            Path reprint = billing.exportFinalBillPdf(stayId);
            System.out.println(
                    "reprint_path="
                            + reprint.toAbsolutePath()
            );
            System.out.println(
                    "reprint_exists="
                            + Files.isRegularFile(reprint)
            );
            System.out.println(
                    "reprint_bytes=" + Files.size(reprint)
            );
            System.out.println(
                    "STEP3_PASS="
                            + (Files.isRegularFile(reprint)
                            && Files.size(reprint) > 0)
            );

            System.out.println(
                    "ALL_PASS="
                            + (stay.getStatus()
                            == ReservationStatus.CHECKED_OUT
                            && latestPdf != null
                            && Files.isRegularFile(latestPdf)
                            && blockedOk
                            && Files.isRegularFile(reprint))
            );

        } catch (Exception e) {
            System.out.println(
                    "VerifyCheckoutFinalBill failed."
            );
            e.printStackTrace();
        } finally {
            AppConfig.shutdown();
        }
    }

    private static String phone(long stamp) {
        return String.format(
                "416-555-%04d",
                Math.floorMod(stamp, 10000)
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(String name)
            throws Exception {
        Field f = AppConfig.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(null);
    }

    private static BillingPaymentService
    billingPaymentService() throws Exception {
        return field("BILLING_PAYMENT_SERVICE");
    }

    private static RoomRepository roomRepository()
            throws Exception {
        return field("ROOM_REPOSITORY");
    }
}
