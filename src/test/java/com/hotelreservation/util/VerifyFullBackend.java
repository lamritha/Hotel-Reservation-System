package com.hotelreservation.util;

import com.hotelreservation.config.AppConfig;
import com.hotelreservation.config.HotelPolicyConfig;
import com.hotelreservation.factory.RoomFactory;
import com.hotelreservation.model.AdminRole;
import com.hotelreservation.model.Billing;
import com.hotelreservation.model.Guest;
import com.hotelreservation.model.LoyaltyAccount;
import com.hotelreservation.model.Payment;
import com.hotelreservation.model.PaymentMethod;
import com.hotelreservation.model.PaymentType;
import com.hotelreservation.model.ReportPeriod;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.ReservationAddOn;
import com.hotelreservation.model.ReservationRoom;
import com.hotelreservation.model.ReservationStatus;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomStatus;
import com.hotelreservation.model.RoomType;
import com.hotelreservation.model.WaitlistEntry;
import com.hotelreservation.model.WaitlistStatus;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.service.BillingPaymentService;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.DiscountService;
import com.hotelreservation.service.FeedbackService;
import com.hotelreservation.service.LoyaltyService;
import com.hotelreservation.service.ReportService;
import com.hotelreservation.service.ReservationManagementService;
import com.hotelreservation.service.ReservationRequest;
import com.hotelreservation.service.RoomManagementService;
import com.hotelreservation.service.WaitlistRequest;
import com.hotelreservation.service.WaitlistService;
import com.hotelreservation.strategy.WeekendPricingStrategy;

import jakarta.persistence.EntityManager;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Comprehensive backend verification against the real H2 database.
 */
public class VerifyFullBackend {

    private static final List<String> RESULTS = new ArrayList<>();
    private static int passed;
    private static int failed;
    private static long stamp;
    private static int phoneSeq;

    private static BillingPaymentService billing;
    private static ReservationManagementService reservations;
    private static DiscountService discounts;
    private static WaitlistService waitlist;
    private static RoomManagementService rooms;
    private static RoomRepository roomRepository;
    private static FeedbackService feedback;
    private static ReportService reports;
    private static LoyaltyService loyalty;

    public static void main(String[] args) {
        try {
            AppConfig.initialize();
            stamp = System.currentTimeMillis();
            phoneSeq = 0;

            wireServices();
            ensureAdminLogin();

            System.out.println("=== VERIFY FULL BACKEND ===");
            System.out.println("stamp=" + stamp);
            System.out.println();

            test01_singleRoomBooking();
            test02_multiRoomBooking();
            test03_bookingWithAddOns();
            test04_weekendMultiplier();
            test05_phoneReservationWithDeposit();
            test06_modifyAndConflict();
            test07_cancelAndAvailability();
            test08_threePaymentMethods();
            test09_refund();
            test10_checkoutBlocked();
            test11_checkoutPaidWithPdf();
            test12_discountCaps();
            test13_waitlistDateMatch();
            test14_feedbackEligibility();
            test15_authPasswords();
            test16_reports();

            System.out.println();
            System.out.println("=== SUMMARY ===");
            for (String line : RESULTS) {
                System.out.println(line);
            }
            System.out.println();
            System.out.println(
                    "PASSED=" + passed + " FAILED=" + failed
                            + " TOTAL=" + (passed + failed)
            );
            System.out.println(
                    failed == 0
                            ? "=== VERIFY RESULT: ALL PASSED ==="
                            : "=== VERIFY RESULT: FAILURES PRESENT ==="
            );
        } catch (Exception e) {
            System.out.println("VerifyFullBackend crashed.");
            e.printStackTrace();
        } finally {
            BookingSession.reset();
            AppConfig.shutdown();
        }
    }

    // ------------------------------------------------------------------
    // 1–4 Kiosk
    // ------------------------------------------------------------------

    private static void test01_singleRoomBooking() {
        String name = "1. Kiosk single-room booking";
        try {
            LocalDate in = LocalDate.of(2027, 9, 10);
            LocalDate out = LocalDate.of(2027, 9, 12);
            BookingSession.reset();
            BookingSession.setCheckInDate(in);
            BookingSession.setCheckOutDate(out);
            BookingSession.setSingleRoomQuantity(1);
            BookingSession.setNumAdults(1);
            BookingSession.setNumChildren(0);

            Guest guest = guest("Single", "One", "single");
            double expectedRoom = expectedRoomTotal(120.0, in, out);
            double expectedTax = round(expectedRoom * HotelPolicyConfig.TAX_RATE);
            double expectedTotal = round(expectedRoom + expectedTax);

            Reservation res = new BookingService().completeBooking(
                    guest, in, out, 1, 0, false, PaymentMethod.CARD
            );

            Billing bill = loadBilling(res.getReservationId());
            List<ReservationRoom> rr = loadReservationRooms(res.getReservationId());

            boolean ok = res.getStatus() == ReservationStatus.CONFIRMED
                    && rr.size() == 1
                    && bill != null
                    && moneyEq(bill.getSubtotal(), expectedRoom)
                    && moneyEq(bill.getTaxAmount(), expectedTax)
                    && moneyEq(bill.getTotalAmount(), expectedTotal);

            record(name, ok,
                    "Reservation+ReservationRoom+Billing; room/tax/total="
                            + expectedRoom + "/" + expectedTax + "/" + expectedTotal,
                    "id=" + res.getReservationId()
                            + " status=" + res.getStatus()
                            + " rooms=" + rr.size()
                            + " subtotal=" + bill.getSubtotal()
                            + " tax=" + bill.getTaxAmount()
                            + " total=" + bill.getTotalAmount());
        } catch (Exception e) {
            recordFail(name, e);
        } finally {
            BookingSession.reset();
        }
    }

    private static void test02_multiRoomBooking() {
        String name = "2. Kiosk multi-room (5 adults)";
        try {
            LocalDate in = LocalDate.of(2027, 9, 20);
            LocalDate out = LocalDate.of(2027, 9, 22);
            BookingSession.reset();
            BookingSession.setCheckInDate(in);
            BookingSession.setCheckOutDate(out);
            BookingSession.setSingleRoomQuantity(1);
            BookingSession.setDoubleRoomQuantity(1);
            BookingSession.setNumAdults(5);
            BookingSession.setNumChildren(0);

            double nightly = 120.0 + 180.0;
            double expectedRoom = expectedRoomTotal(nightly, in, out);
            double expectedTax = round(expectedRoom * HotelPolicyConfig.TAX_RATE);
            double expectedTotal = round(expectedRoom + expectedTax);

            Guest guest = guest("Multi", "Five", "multi");
            Reservation res = new BookingService().completeBooking(
                    guest, in, out, 5, 0, true, PaymentMethod.CARD
            );

            List<ReservationRoom> rr = loadReservationRooms(res.getReservationId());
            int adultsSum = rr.stream().mapToInt(ReservationRoom::getAssignedAdults).sum();
            Billing bill = loadBilling(res.getReservationId());

            boolean ok = rr.size() == 2
                    && adultsSum == 5
                    && moneyEq(bill.getTotalAmount(), expectedTotal);

            record(name, ok,
                    "2 ReservationRooms, adults sum=5, total=" + expectedTotal,
                    "rooms=" + rr.size()
                            + " adultsSum=" + adultsSum
                            + " total=" + bill.getTotalAmount()
                            + " allocations=" + describeAllocations(rr));
        } catch (Exception e) {
            recordFail(name, e);
        } finally {
            BookingSession.reset();
        }
    }

    private static void test03_bookingWithAddOns() {
        String name = "3. Kiosk booking with add-ons";
        try {
            LocalDate in = LocalDate.of(2027, 10, 1);
            LocalDate out = LocalDate.of(2027, 10, 4);
            long nights = 3;
            BookingSession.reset();
            BookingSession.setCheckInDate(in);
            BookingSession.setCheckOutDate(out);
            BookingSession.setSingleRoomQuantity(1);
            BookingSession.setWifiSelected(true);
            BookingSession.setBreakfastSelected(true);

            double expectedRoom = expectedRoomTotal(120.0, in, out);
            double expectedAddOns = HotelPolicyConfig.WIFI_PRICE
                    + HotelPolicyConfig.BREAKFAST_PRICE_PER_NIGHT * nights;
            double expectedSubtotal = round(expectedRoom + expectedAddOns);
            double expectedTax = round(expectedSubtotal * HotelPolicyConfig.TAX_RATE);
            double expectedTotal = round(expectedSubtotal + expectedTax);

            Guest guest = guest("Addon", "Guest", "addon");
            Reservation res = new BookingService().completeBooking(
                    guest, in, out, 1, 0, false, PaymentMethod.CARD
            );

            List<ReservationAddOn> addOns = loadAddOns(res.getReservationId());
            Billing bill = loadBilling(res.getReservationId());
            boolean hasWifi = addOns.stream()
                    .anyMatch(a -> "Wi-Fi".equalsIgnoreCase(a.getAddOn().getName()));
            boolean hasBreakfast = addOns.stream()
                    .anyMatch(a -> "Breakfast".equalsIgnoreCase(a.getAddOn().getName()));

            boolean ok = addOns.size() == 2
                    && hasWifi
                    && hasBreakfast
                    && moneyEq(bill.getSubtotal(), expectedSubtotal)
                    && moneyEq(bill.getTotalAmount(), expectedTotal);

            record(name, ok,
                    "Wi-Fi+Breakfast rows; subtotal/total="
                            + expectedSubtotal + "/" + expectedTotal,
                    "addonRows=" + addOns.size()
                            + " names=" + addOns.stream()
                            .map(a -> a.getAddOn().getName())
                            .toList()
                            + " subtotal=" + bill.getSubtotal()
                            + " total=" + bill.getTotalAmount());
        } catch (Exception e) {
            recordFail(name, e);
        } finally {
            BookingSession.reset();
        }
    }

    private static void test04_weekendMultiplier() {
        String name = "4. Weekend Fri/Sat 1.25x (not full-stay)";
        try {
            // Fri 2027-03-05 → Sun 2027-03-07: Fri + Sat nights, off-peak
            LocalDate in = LocalDate.of(2027, 3, 5);
            LocalDate out = LocalDate.of(2027, 3, 7);
            if (in.getDayOfWeek() != DayOfWeek.FRIDAY) {
                throw new IllegalStateException("fixture date is not Friday");
            }

            BookingSession.reset();
            BookingSession.setCheckInDate(in);
            BookingSession.setCheckOutDate(out);
            BookingSession.setSingleRoomQuantity(1);

            double weekdayFullStay = 120.0 * 2; // wrong if applied as flat stay
            double weekendFlatStay = 120.0 * 1.25 * 2; // same as per-night here but intentional check
            double expectedRoom = 120.0 * HotelPolicyConfig.WEEKEND_MULTIPLIER
                    + 120.0 * HotelPolicyConfig.WEEKEND_MULTIPLIER;
            expectedRoom = round(expectedRoom);
            // also verify via strategy
            double viaStrategy = round(new WeekendPricingStrategy()
                    .calculatePrice(120.0, in, out));

            Guest guest = guest("Weekend", "Stay", "weekend");
            Reservation res = new BookingService().completeBooking(
                    guest, in, out, 1, 0, false, PaymentMethod.CARD
            );
            Billing bill = loadBilling(res.getReservationId());

            boolean ok = moneyEq(viaStrategy, expectedRoom)
                    && moneyEq(bill.getSubtotal(), expectedRoom)
                    && moneyEq(expectedRoom, weekendFlatStay)
                    && !moneyEq(bill.getSubtotal(), weekdayFullStay);

            record(name, ok,
                    "room subtotal=300.00 (120*1.25 Fri + 120*1.25 Sat), not 240 weekday",
                    "subtotal=" + bill.getSubtotal()
                            + " strategy=" + viaStrategy
                            + " weekdayWouldBe=" + weekdayFullStay);
        } catch (Exception e) {
            recordFail(name, e);
        } finally {
            BookingSession.reset();
        }
    }

    // ------------------------------------------------------------------
    // 5–7 Admin reservations
    // ------------------------------------------------------------------

    private static void test05_phoneReservationWithDeposit() {
        String name = "5. Phone reservation + deposit";
        try {
            LocalDate in = LocalDate.of(2027, 11, 1);
            LocalDate out = LocalDate.of(2027, 11, 3);
            Room room = requireRoom(null, in, out);
            Reservation res = reservations.createPhoneReservation(
                    request("Phone", "Deposit", "phone.dep", in, out, 1, 0,
                            List.of(room.getRoomId()))
            );
            BillingPaymentService.BillingSummary before =
                    billing.getSummary(res.getReservationId());
            double deposit = round(Math.min(50.0, before.outstanding() / 2));

            Payment pay = billing.processPayment(
                    res.getReservationId(),
                    deposit,
                    PaymentMethod.CARD,
                    PaymentType.DEPOSIT,
                    "Deposit collected at booking"
            );

            BillingPaymentService.BillingSummary after =
                    billing.getSummary(res.getReservationId());
            List<Payment> payments = billing.getPayments(
                    after.billing().getBillingId()
            );

            boolean ok = res.getStatus() == ReservationStatus.CONFIRMED
                    && pay.getPaymentType() == PaymentType.DEPOSIT
                    && moneyEq(pay.getAmount(), deposit)
                    && payments.size() == 1
                    && moneyEq(after.paid(), deposit)
                    && moneyEq(after.outstanding(), before.outstanding() - deposit);

            record(name, ok,
                    "CONFIRMED + DEPOSIT payment persisted; outstanding reduced",
                    "id=" + res.getReservationId()
                            + " deposit=" + pay.getAmount()
                            + " type=" + pay.getPaymentType()
                            + " paid=" + after.paid()
                            + " outstanding=" + after.outstanding());
        } catch (Exception e) {
            recordFail(name, e);
        }
    }

    private static void test06_modifyAndConflict() {
        String name = "6. Modify reservation + conflict reject";
        try {
            LocalDate inA = LocalDate.of(2027, 11, 10);
            LocalDate outA = LocalDate.of(2027, 11, 12);
            LocalDate inB = LocalDate.of(2027, 11, 14);
            LocalDate outB = LocalDate.of(2027, 11, 16);

            Room room = requireRoom(null, inA, outB);
            Reservation holder = reservations.createPhoneReservation(
                    request("Hold", "Room", "hold.room", inA, outA, 1, 0,
                            List.of(room.getRoomId()))
            );
            Reservation editable = reservations.createPhoneReservation(
                    request("Edit", "Me", "edit.me", inB, outB, 1, 0,
                            List.of(room.getRoomId()))
            );

            LocalDate newIn = LocalDate.of(2027, 11, 15);
            LocalDate newOut = LocalDate.of(2027, 11, 17);
            Reservation modified = reservations.modifyReservation(
                    editable.getReservationId(),
                    request("Edit", "Me", "edit.me", newIn, newOut, 1, 0,
                            List.of(room.getRoomId()))
            );

            boolean conflictRejected = false;
            String conflictMsg = null;
            try {
                reservations.modifyReservation(
                        editable.getReservationId(),
                        request("Edit", "Me", "edit.me", inA, outA, 1, 0,
                                List.of(room.getRoomId()))
                );
            } catch (RuntimeException ex) {
                conflictRejected = true;
                conflictMsg = rootMessage(ex);
            }

            Reservation reloaded = loadReservation(editable.getReservationId());
            boolean ok = modified.getCheckInDate().equals(newIn)
                    && modified.getCheckOutDate().equals(newOut)
                    && reloaded.getCheckInDate().equals(newIn)
                    && conflictRejected
                    && conflictMsg != null
                    && conflictMsg.contains("no longer available");

            record(name, ok,
                    "date change persists; overlapping modify rejected",
                    "holder=" + holder.getReservationId()
                            + " edited=" + editable.getReservationId()
                            + " newDates=" + reloaded.getCheckInDate()
                            + ".." + reloaded.getCheckOutDate()
                            + " conflictRejected=" + conflictRejected
                            + " msg=" + conflictMsg);
        } catch (Exception e) {
            recordFail(name, e);
        }
    }

    private static void test07_cancelAndAvailability() {
        String name = "7. Cancel reservation frees room";
        try {
            LocalDate in = LocalDate.of(2027, 12, 1);
            LocalDate out = LocalDate.of(2027, 12, 3);
            Room room = requireRoom(null, in, out);
            Long roomId = room.getRoomId();

            Reservation res = reservations.createPhoneReservation(
                    request("Cancel", "Me", "cancel.me", in, out, 1, 0,
                            List.of(roomId))
            );

            List<Room> duringHold = roomRepository.findAvailableRoomsForDates(
                    in, out, null
            );
            boolean blockedWhileHeld = duringHold.stream()
                    .noneMatch(r -> Objects.equals(r.getRoomId(), roomId));

            Reservation cancelled = reservations.cancelReservation(
                    res.getReservationId()
            );

            List<Room> afterCancel = roomRepository.findAvailableRoomsForDates(
                    in, out, null
            );
            boolean freeAgain = afterCancel.stream()
                    .anyMatch(r -> Objects.equals(r.getRoomId(), roomId));

            boolean ok = cancelled.getStatus() == ReservationStatus.CANCELLED
                    && blockedWhileHeld
                    && freeAgain;

            record(name, ok,
                    "status CANCELLED; room absent while held, present after cancel",
                    "id=" + res.getReservationId()
                            + " status=" + cancelled.getStatus()
                            + " blockedWhileHeld=" + blockedWhileHeld
                            + " freeAgain=" + freeAgain);
        } catch (Exception e) {
            recordFail(name, e);
        }
    }

    // ------------------------------------------------------------------
    // 8–11 Billing
    // ------------------------------------------------------------------

    private static void test08_threePaymentMethods() {
        String name = "8. Cash / Card / Loyalty payments";
        try {
            LocalDate in = LocalDate.of(2028, 1, 10);
            LocalDate out = LocalDate.of(2028, 1, 12);

            Reservation cashRes = createPhoneOnFreshRoom(
                    "Pay", "Cash", "pay.cash", in, out
            );
            Reservation cardRes = createPhoneOnFreshRoom(
                    "Pay", "Card", "pay.card",
                    in.plusDays(3), out.plusDays(3)
            );
            Reservation loyRes = createPhoneOnFreshRoom(
                    "Pay", "Loy", "pay.loy",
                    in.plusDays(6), out.plusDays(6)
            );

            double cashAmt = 25.0;
            Payment cashPay = billing.processPayment(
                    cashRes.getReservationId(), cashAmt,
                    PaymentMethod.CASH, PaymentType.PARTIAL_PAYMENT, "cash test"
            );

            double cardAmt = 40.0;
            Payment cardPay = billing.processPayment(
                    cardRes.getReservationId(), cardAmt,
                    PaymentMethod.CARD, PaymentType.PARTIAL_PAYMENT, "card test"
            );

            Guest loyGuest = loadReservation(loyRes.getReservationId()).getGuest();
            loyalty.enrollGuest(loyGuest, "VERIFY");
            grantLoyaltyPoints(loyGuest.getGuestId(), 50_000);
            BillingPaymentService.BillingSummary loySum =
                    billing.getSummary(loyRes.getReservationId());
            double loyAmt = round(
                    loySum.outstanding() * HotelPolicyConfig.LOYALTY_REDEMPTION_CAP
            );
            Payment loyPay = billing.processPayment(
                    loyRes.getReservationId(), loyAmt,
                    PaymentMethod.LOYALTY_POINTS, PaymentType.PARTIAL_PAYMENT,
                    "loyalty test"
            );

            boolean ok = cashPay.getPaymentMethod() == PaymentMethod.CASH
                    && moneyEq(cashPay.getAmount(), cashAmt)
                    && cardPay.getPaymentMethod() == PaymentMethod.CARD
                    && moneyEq(cardPay.getAmount(), cardAmt)
                    && loyPay.getPaymentMethod() == PaymentMethod.LOYALTY_POINTS
                    && loyPay.getPaymentType() == PaymentType.LOYALTY_REDEMPTION
                    && moneyEq(loyPay.getAmount(), loyAmt);

            record(name, ok,
                    "three Payment rows with correct method/type/amount",
                    "cash=" + cashPay.getPaymentMethod() + "/" + cashPay.getAmount()
                            + " card=" + cardPay.getPaymentMethod() + "/" + cardPay.getAmount()
                            + " loy=" + loyPay.getPaymentMethod() + "/"
                            + loyPay.getPaymentType() + "/" + loyPay.getAmount());
        } catch (Exception e) {
            recordFail(name, e);
        }
    }

    private static void test09_refund() {
        String name = "9. Refund increases outstanding";
        try {
            LocalDate in = LocalDate.of(2028, 2, 1);
            LocalDate out = LocalDate.of(2028, 2, 3);
            Reservation res = createPhoneOnFreshRoom(
                    "Ref", "Und", "refund.me", in, out
            );
            BillingPaymentService.BillingSummary beforePay =
                    billing.getSummary(res.getReservationId());
            double payAmt = 60.0;
            billing.processPayment(
                    res.getReservationId(), payAmt,
                    PaymentMethod.CARD, PaymentType.PARTIAL_PAYMENT, "pre-refund"
            );
            BillingPaymentService.BillingSummary afterPay =
                    billing.getSummary(res.getReservationId());

            double refundAmt = 20.0;
            Payment refund = billing.refund(
                    res.getReservationId(), refundAmt,
                    PaymentMethod.CARD, "refund test"
            );
            BillingPaymentService.BillingSummary afterRefund =
                    billing.getSummary(res.getReservationId());

            boolean ok = refund.getPaymentType() == PaymentType.REFUND
                    && refund.getAmount() < 0
                    && moneyEq(refund.getAmount(), -refundAmt)
                    && moneyEq(
                    afterRefund.outstanding(),
                    afterPay.outstanding() + refundAmt
            )
                    && moneyEq(
                    afterRefund.paid(),
                    afterPay.paid() - refundAmt
            );

            record(name, ok,
                    "negative REFUND payment; outstanding up by refund amount",
                    "beforeOut=" + beforePay.outstanding()
                            + " afterPayOut=" + afterPay.outstanding()
                            + " refundAmt=" + refund.getAmount()
                            + " afterRefundOut=" + afterRefund.outstanding()
                            + " paid=" + afterRefund.paid());
        } catch (Exception e) {
            recordFail(name, e);
        }
    }

    private static void test10_checkoutBlocked() {
        String name = "10. Checkout blocked with outstanding";
        try {
            LocalDate in = LocalDate.of(2028, 2, 10);
            LocalDate out = LocalDate.of(2028, 2, 12);
            Reservation res = createPhoneOnFreshRoom(
                    "Block", "Checkout", "block.co", in, out
            );
            billing.checkIn(res.getReservationId());
            BillingPaymentService.BillingSummary sum =
                    billing.getSummary(res.getReservationId());

            boolean blocked = false;
            String msg = null;
            try {
                billing.checkout(res.getReservationId());
            } catch (RuntimeException ex) {
                blocked = true;
                msg = rootMessage(ex);
            }

            Reservation after = loadReservation(res.getReservationId());
            boolean ok = sum.outstanding() > 0.009
                    && blocked
                    && msg != null
                    && msg.contains("Checkout is blocked")
                    && after.getStatus() == ReservationStatus.CHECKED_IN;

            record(name, ok,
                    "checkout throws; status stays CHECKED_IN",
                    "outstanding=" + sum.outstanding()
                            + " blocked=" + blocked
                            + " msg=" + msg
                            + " status=" + after.getStatus());
        } catch (Exception e) {
            recordFail(name, e);
        }
    }

    private static Long checkedOutForFeedbackId;

    private static void test11_checkoutPaidWithPdf() {
        String name = "11. Checkout fully paid + PDF";
        try {
            LocalDate in = LocalDate.of(2028, 2, 20);
            LocalDate out = LocalDate.of(2028, 2, 22);
            Reservation res = createPhoneOnFreshRoom(
                    "Full", "Pay", "full.pay", in, out
            );
            Long resId = res.getReservationId();
            Long roomId = res.getRoom().getRoomId();

            BillingPaymentService.BillingSummary sum =
                    billing.getSummary(resId);
            billing.processPayment(
                    resId, sum.outstanding(),
                    PaymentMethod.CARD, PaymentType.FINAL_PAYMENT, "settle"
            );
            billing.checkIn(resId);
            Reservation checkedOut = billing.checkout(resId);

            Path foundPdf = null;
            Path exportsDir = Path.of("exports");
            if (Files.isDirectory(exportsDir)) {
                try (var stream = Files.list(exportsDir)) {
                    foundPdf = stream
                            .filter(p -> p.getFileName().toString()
                                    .startsWith("final-bill-RES-" + resId))
                            .findFirst()
                            .orElse(null);
                }
            }

            Room roomAfter = roomRepository.findById(roomId);
            boolean ok = checkedOut.getStatus() == ReservationStatus.CHECKED_OUT
                    && roomAfter.getStatus() == RoomStatus.AVAILABLE
                    && foundPdf != null
                    && Files.exists(foundPdf)
                    && Files.size(foundPdf) > 0;

            checkedOutForFeedbackId = resId;

            record(name, ok,
                    "CHECKED_OUT, room AVAILABLE, PDF on disk",
                    "status=" + checkedOut.getStatus()
                            + " roomStatus=" + roomAfter.getStatus()
                            + " pdf=" + foundPdf
                            + " size=" + (foundPdf == null ? 0 : Files.size(foundPdf)));
        } catch (Exception e) {
            recordFail(name, e);
        }
    }

    // ------------------------------------------------------------------
    // 12 Discounts
    // ------------------------------------------------------------------

    private static void test12_discountCaps() {
        String name = "12. Discount caps ADMIN/MANAGER";
        try {
            LocalDate in = LocalDate.of(2028, 3, 1);
            LocalDate out = LocalDate.of(2028, 3, 3);

            Reservation adminOkRes = createPhoneOnFreshRoom(
                    "Disc", "AdminOk", "disc.aok", in, out
            );
            Reservation adminFailRes = createPhoneOnFreshRoom(
                    "Disc", "AdminOver", "disc.aover",
                    in.plusDays(4), out.plusDays(4)
            );

            discounts.applyDiscount(
                    adminOkRes.getReservationId(), 15.0, "admin max ok"
            );
            boolean admin16Rejected = false;
            String admin16Msg = null;
            try {
                discounts.applyDiscount(
                        adminFailRes.getReservationId(), 16.0, "admin over"
                );
            } catch (RuntimeException ex) {
                admin16Rejected = true;
                admin16Msg = rootMessage(ex);
            }

            AppConfig.getAuthenticationService().logout();
            var mgrLogin = AppConfig.getAuthenticationService()
                    .authenticate(
                            "manager",
                            "ChangeMe!2026Manager",
                            AdminRole.MANAGER
                    );
            if (!mgrLogin.successful()) {
                throw new IllegalStateException(
                        "manager login failed: " + mgrLogin.message()
                );
            }

            Reservation mgrOkRes = createPhoneOnFreshRoom(
                    "Disc", "MgrOk", "disc.mok",
                    in.plusDays(8), out.plusDays(8)
            );
            Reservation mgrFailRes = createPhoneOnFreshRoom(
                    "Disc", "MgrOver", "disc.mover",
                    in.plusDays(12), out.plusDays(12)
            );

            discounts.applyDiscount(
                    mgrOkRes.getReservationId(), 30.0, "manager max ok"
            );
            boolean mgr31Rejected = false;
            String mgr31Msg = null;
            try {
                discounts.applyDiscount(
                        mgrFailRes.getReservationId(), 31.0, "manager over"
                );
            } catch (RuntimeException ex) {
                mgr31Rejected = true;
                mgr31Msg = rootMessage(ex);
            }

            // restore admin session for remaining tests
            AppConfig.getAuthenticationService().logout();
            ensureAdminLogin();

            boolean ok = admin16Rejected
                    && admin16Msg != null
                    && admin16Msg.contains("15%")
                    && mgr31Rejected
                    && mgr31Msg != null
                    && mgr31Msg.contains("30%");

            record(name, ok,
                    "ADMIN 15% ok / 16% reject; MANAGER 30% ok / 31% reject",
                    "admin16Rejected=" + admin16Rejected
                            + " msg=" + admin16Msg
                            + " mgr31Rejected=" + mgr31Rejected
                            + " msg=" + mgr31Msg);
        } catch (Exception e) {
            try {
                AppConfig.getAuthenticationService().logout();
                ensureAdminLogin();
            } catch (Exception ignored) {
            }
            recordFail(name, e);
        }
    }

    // ------------------------------------------------------------------
    // 13 Waitlist
    // ------------------------------------------------------------------

    private static void test13_waitlistDateMatch() {
        String name = "13. Waitlist overlap notify";
        try {
            // Mirror VerifyWaitlistDateMatch: free window [today, nextBookingStart)
            LocalDate availableFrom = LocalDate.now();
            LocalDate nextStart = LocalDate.of(2027, 8, 20);
            LocalDate nextEnd = LocalDate.of(2027, 8, 22);
            LocalDate nonOverlapIn = LocalDate.of(2027, 9, 1);
            LocalDate nonOverlapOut = LocalDate.of(2027, 9, 5);
            LocalDate overlapIn = LocalDate.of(2027, 8, 10);
            LocalDate overlapOut = LocalDate.of(2027, 8, 15);

            if (!availableFrom.isBefore(nextStart)) {
                throw new IllegalStateException(
                        "today is on/after nextStart; adjust dates"
                );
            }

            Room target = requireCleanRoom(
                    RoomType.PENTHOUSE,
                    availableFrom,
                    nextStart,
                    nextEnd
            );
            reservations.createPhoneReservation(
                    request("Next", "Penthouse", "next.ph", nextStart, nextEnd,
                            1, 0, List.of(target.getRoomId()))
            );

            WaitlistEntry nonOverlap = waitlist.add(
                    new WaitlistRequest(
                            "Wait", "Non",
                            "wl.non" + stamp + "@email.com",
                            nextPhone(), "1 Non St",
                            RoomType.PENTHOUSE,
                            nonOverlapIn, nonOverlapOut, 1, 0
                    )
            );
            WaitlistEntry overlap = waitlist.add(
                    new WaitlistRequest(
                            "Wait", "Yes",
                            "wl.yes" + stamp + "@email.com",
                            nextPhone(), "1 Yes St",
                            RoomType.PENTHOUSE,
                            overlapIn, overlapOut, 1, 0
                    )
            );

            var nextStartFound = roomRepository.findNextReservationStart(
                    target, availableFrom
            );

            Room managed = roomRepository.findById(target.getRoomId());
            rooms.changeStatus(managed, RoomStatus.MAINTENANCE);
            managed = roomRepository.findById(target.getRoomId());
            rooms.changeStatus(managed, RoomStatus.AVAILABLE);

            WaitlistEntry reloadedNon = loadWaitlist(nonOverlap.getWaitlistId());
            WaitlistEntry reloadedYes = loadWaitlist(overlap.getWaitlistId());

            boolean ok = reloadedNon.getStatus() == WaitlistStatus.WAITING
                    && reloadedYes.getStatus() == WaitlistStatus.NOTIFIED;

            record(name, ok,
                    "non-overlap stays WAITING; overlap → NOTIFIED",
                    "non=" + reloadedNon.getStatus()
                            + " overlap=" + reloadedYes.getStatus()
                            + " room=" + target.getRoomNumber()
                            + " nextStartFound=" + nextStartFound.orElse(null)
                            + " expectedNext=" + nextStart);
        } catch (Exception e) {
            recordFail(name, e);
        }
    }

    // ------------------------------------------------------------------
    // 14 Feedback
    // ------------------------------------------------------------------

    private static void test14_feedbackEligibility() {
        String name = "14. Feedback eligibility";
        try {
            if (checkedOutForFeedbackId == null) {
                throw new IllegalStateException(
                        "test 11 did not produce a checked-out reservation"
                );
            }

            var submitted = feedback.submit(
                    checkedOutForFeedbackId, 5, "Great stay verify"
            );

            LocalDate in = LocalDate.of(2028, 4, 1);
            LocalDate out = LocalDate.of(2028, 4, 3);
            Reservation notOut = createPhoneOnFreshRoom(
                    "No", "Feedback", "nofb", in, out
            );

            boolean blocked = false;
            String msg = null;
            try {
                feedback.submit(
                        notOut.getReservationId(), 4, "too early"
                );
            } catch (RuntimeException ex) {
                blocked = true;
                msg = rootMessage(ex);
            }

            boolean ok = submitted != null
                    && submitted.getRating() == 5
                    && blocked
                    && msg != null
                    && msg.toLowerCase().contains("checkout");

            record(name, ok,
                    "CHECKED_OUT submit ok; non-checked-out blocked",
                    "feedbackId=" + submitted.getFeedbackId()
                            + " blocked=" + blocked
                            + " msg=" + msg);
        } catch (Exception e) {
            recordFail(name, e);
        }
    }

    // ------------------------------------------------------------------
    // 15 Auth
    // ------------------------------------------------------------------

    private static void test15_authPasswords() {
        String name = "15. Auth new vs old password";
        try {
            AppConfig.getAuthenticationService().logout();

            var newOk = AppConfig.getAuthenticationService()
                    .authenticate(
                            "admin",
                            "ChangeMe!2026Admin",
                            AdminRole.ADMIN
                    );
            AppConfig.getAuthenticationService().logout();

            var oldFail = AppConfig.getAuthenticationService()
                    .authenticate(
                            "admin",
                            "admin123",
                            AdminRole.ADMIN
                    );

            // restore session
            ensureAdminLogin();

            boolean ok = newOk.successful()
                    && !oldFail.successful();

            record(name, ok,
                    "new password succeeds; old admin123 fails",
                    "newOk=" + newOk.successful()
                            + " oldFailSuccessful=" + oldFail.successful()
                            + " oldMsg=" + oldFail.message());
        } catch (Exception e) {
            try {
                ensureAdminLogin();
            } catch (Exception ignored) {
            }
            recordFail(name, e);
        }
    }

    // ------------------------------------------------------------------
    // 16 Reports
    // ------------------------------------------------------------------

    private static void test16_reports() {
        String name = "16. Revenue + occupancy reports";
        try {
            LocalDate today = LocalDate.now();
            List<ReportService.RevenueReportRow> revenue =
                    reports.revenue(
                            today, today, ReportPeriod.DAILY, null
                    );
            List<ReportService.OccupancyReportRow> occupancy =
                    reports.occupancy(
                            today, today.plusDays(7),
                            ReportPeriod.DAILY, null
                    );

            long revenueReservations = revenue.stream()
                    .mapToLong(ReportService.RevenueReportRow::reservationCount)
                    .sum();
            double revenueTotal = revenue.stream()
                    .mapToDouble(ReportService.RevenueReportRow::total)
                    .sum();

            // Count billings created today in DB for ground truth
            long dbBillingsToday = countBillingsCreatedOn(today);

            boolean ok = !revenue.isEmpty()
                    && revenueReservations >= 1
                    && revenueReservations <= dbBillingsToday + 2
                    // allow small variance from grouping uniqueness
                    && revenueTotal > 0
                    && !occupancy.isEmpty()
                    && occupancy.getFirst().roomsAvailable() >= 0;

            // Stronger check: today's revenue reservation count matches DB billings today
            boolean strong = revenueReservations == dbBillingsToday;

            record(name, ok && strong,
                    "revenue reflects today's created billings; occupancy returns rows",
                    "revenueRows=" + revenue.size()
                            + " reservationCount=" + revenueReservations
                            + " dbBillingsToday=" + dbBillingsToday
                            + " revenueTotal=" + round(revenueTotal)
                            + " occupancyRows=" + occupancy.size()
                            + " sampleOcc=" + (occupancy.isEmpty()
                            ? "n/a"
                            : occupancy.getFirst()));
        } catch (Exception e) {
            recordFail(name, e);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void wireServices() throws Exception {
        reservations = AppConfig.getReservationManagementService();
        billing = field("BILLING_PAYMENT_SERVICE");
        discounts = field("DISCOUNT_SERVICE");
        waitlist = field("WAITLIST_SERVICE");
        rooms = field("ROOM_MANAGEMENT_SERVICE");
        roomRepository = field("ROOM_REPOSITORY");
        feedback = field("FEEDBACK_SERVICE");
        reports = field("REPORT_SERVICE");
        loyalty = field("LOYALTY_SERVICE");
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(String name) throws Exception {
        Field f = AppConfig.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(null);
    }

    private static void ensureAdminLogin() {
        var login = AppConfig.getAuthenticationService()
                .authenticate(
                        "admin",
                        "ChangeMe!2026Admin",
                        AdminRole.ADMIN
                );
        if (!login.successful()) {
            throw new IllegalStateException(
                    "admin login failed: " + login.message()
            );
        }
    }

    private static Guest guest(String first, String last, String key) {
        return new Guest(
                first,
                last,
                key + stamp + "@email.com",
                nextPhone(),
                "1 " + first + " St"
        );
    }

    private static ReservationRequest request(
            String first,
            String last,
            String key,
            LocalDate in,
            LocalDate out,
            int adults,
            int children,
            List<Long> roomIds
    ) {
        return new ReservationRequest(
                first,
                last,
                key + stamp + "@email.com",
                nextPhone(),
                "1 " + first + " St",
                in,
                out,
                adults,
                children,
                roomIds
        );
    }

    private static String nextPhone() {
        phoneSeq++;
        return String.format("416-555-%04d", (stamp + phoneSeq) % 10000);
    }

    private static Room requireRoom(
            RoomType type,
            LocalDate in,
            LocalDate out
    ) {
        List<Room> available = roomRepository
                .findAvailableRoomsByTypeAndDates(type, in, out, 1);
        if (available.isEmpty()) {
            throw new IllegalStateException(
                    "No available room type=" + type + " for " + in + ".." + out
            );
        }
        return available.getFirst();
    }

    /**
     * Room free for nextStart..nextEnd with no CONFIRMED/CHECKED_IN stay
     * starting between availableFrom and nextStart (so waitlist window is clean).
     */
    private static Room requireCleanRoom(
            RoomType type,
            LocalDate availableFrom,
            LocalDate nextStart,
            LocalDate nextEnd
    ) {
        List<Room> candidates = roomRepository
                .findAvailableRoomsByTypeAndDates(type, nextStart, nextEnd, 20);
        for (Room candidate : candidates) {
            var next = roomRepository.findNextReservationStart(
                    candidate, availableFrom
            );
            if (next.isEmpty() || !next.get().isBefore(nextStart)) {
                return candidate;
            }
        }

        // Persist a brand-new room so earlier DB bookings cannot shrink the window
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            String number = "V" + (stamp % 100000);
            Room created = new RoomFactory().createRoom(
                    type, number, 9
            );
            em.persist(created);
            em.getTransaction().commit();
            return roomRepository.findById(created.getRoomId());
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    private static Reservation createPhoneOnFreshRoom(
            String first,
            String last,
            String key,
            LocalDate in,
            LocalDate out
    ) {
        Room room = requireRoom(null, in, out);
        return reservations.createPhoneReservation(
                request(first, last, key, in, out, 1, 0,
                        List.of(room.getRoomId()))
        );
    }

    private static double expectedRoomTotal(
            double nightly,
            LocalDate in,
            LocalDate out
    ) {
        return round(new WeekendPricingStrategy()
                .calculatePrice(nightly, in, out));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static boolean moneyEq(double a, double b) {
        return Math.abs(a - b) < 0.015;
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        return c.getMessage();
    }

    private static void record(
            String name,
            boolean ok,
            String expected,
            String actual
    ) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        String line = (ok ? "PASS" : "FAIL") + " | " + name
                + " | expected: " + expected
                + " | actual: " + actual;
        RESULTS.add(line);
        System.out.println(line);
        System.out.println();
    }

    private static void recordFail(String name, Exception e) {
        failed++;
        String line = "FAIL | " + name
                + " | expected: no exception"
                + " | actual: " + rootMessage(e);
        RESULTS.add(line);
        System.out.println(line);
        e.printStackTrace(System.out);
        System.out.println();
    }

    private static Billing loadBilling(Long reservationId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT b FROM Billing b WHERE b.reservation.reservationId = :id",
                            Billing.class
                    )
                    .setParameter("id", reservationId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    private static List<ReservationRoom> loadReservationRooms(Long reservationId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT rr FROM ReservationRoom rr "
                                    + "JOIN FETCH rr.room "
                                    + "WHERE rr.reservation.reservationId = :id",
                            ReservationRoom.class
                    )
                    .setParameter("id", reservationId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    private static List<ReservationAddOn> loadAddOns(Long reservationId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT ra FROM ReservationAddOn ra "
                                    + "JOIN FETCH ra.addOn "
                                    + "WHERE ra.reservation.reservationId = :id",
                            ReservationAddOn.class
                    )
                    .setParameter("id", reservationId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    private static Reservation loadReservation(Long id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT r FROM Reservation r "
                                    + "JOIN FETCH r.guest "
                                    + "JOIN FETCH r.room "
                                    + "WHERE r.reservationId = :id",
                            Reservation.class
                    )
                    .setParameter("id", id)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    private static WaitlistEntry loadWaitlist(Long id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT e FROM WaitlistEntry e WHERE e.waitlistId = :id",
                            WaitlistEntry.class
                    )
                    .setParameter("id", id)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    private static void grantLoyaltyPoints(Long guestId, int points) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            LoyaltyAccount account = em.createQuery(
                            "SELECT a FROM LoyaltyAccount a WHERE a.guest.guestId = :gid",
                            LoyaltyAccount.class
                    )
                    .setParameter("gid", guestId)
                    .getSingleResult();
            account.setPointsBalance(points);
            em.merge(account);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    private static long countBillingsCreatedOn(LocalDate day) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT COUNT(b) FROM Billing b "
                                    + "WHERE b.createdAt >= :from AND b.createdAt < :to",
                            Long.class
                    )
                    .setParameter("from", day.atStartOfDay())
                    .setParameter("to", day.plusDays(1).atStartOfDay())
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    private static String describeAllocations(List<ReservationRoom> rr) {
        StringBuilder sb = new StringBuilder();
        for (ReservationRoom r : rr) {
            if (!sb.isEmpty()) {
                sb.append("; ");
            }
            sb.append(r.getRoom().getRoomType())
                    .append(" a=")
                    .append(r.getAssignedAdults())
                    .append(" c=")
                    .append(r.getAssignedChildren());
        }
        return sb.toString();
    }
}
