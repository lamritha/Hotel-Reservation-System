package com.hotelreservation.service;

import com.hotelreservation.events.AvailabilityEventPublisher;
import com.hotelreservation.model.AdminNotification;
import com.hotelreservation.model.AdminRole;
import com.hotelreservation.model.Billing;
import com.hotelreservation.model.NotificationType;
import com.hotelreservation.model.Payment;
import com.hotelreservation.model.PaymentMethod;
import com.hotelreservation.model.PaymentType;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.ReservationRoom;
import com.hotelreservation.model.ReservationStatus;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomStatus;
import com.hotelreservation.repository.BillingRepository;
import com.hotelreservation.repository.NotificationRepository;
import com.hotelreservation.repository.PaymentRepository;
import com.hotelreservation.repository.ReservationRepository;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.strategy.BillingCalculationStrategy;
import com.hotelreservation.strategy.DiscountBillingStrategy;
import com.hotelreservation.strategy.LoyaltyBillingStrategy;
import com.hotelreservation.strategy.StandardBillingStrategy;
import com.hotelreservation.util.AppLogger;
import com.hotelreservation.util.JpaUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BillingPaymentService {

    private static final Logger LOGGER =
            AppLogger.getLogger(
                    BillingPaymentService.class
            );

    private final BillingRepository billingRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository
            reservationRepository;
    private final RoomRepository roomRepository;
    private final LoyaltyService loyaltyService;
    private final NotificationRepository
            notificationRepository;
    private final AvailabilityEventPublisher publisher;
    private final AdminSession adminSession;

    public BillingPaymentService(
            BillingRepository billingRepository,
            PaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            LoyaltyService loyaltyService,
            NotificationRepository notificationRepository,
            AvailabilityEventPublisher publisher,
            AdminSession adminSession
    ) {
        this.billingRepository = billingRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.loyaltyService = loyaltyService;
        this.notificationRepository =
                notificationRepository;
        this.publisher = publisher;
        this.adminSession = adminSession;
    }

    public List<BillingSummary> search(String keyword) {
        adminSession.requireCurrentUser();
        List<BillingSummary> summaries = new ArrayList<>();
        for (Billing billing :
                billingRepository.search(keyword)) {
            summaries.add(toSummary(billing));
        }
        return summaries;
    }

    public BillingSummary getSummary(
            Long reservationId
    ) {
        adminSession.requireCurrentUser();
        Billing billing =
                billingRepository
                        .findByReservationIdWithGuest(
                                reservationId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Billing record was not found."
                                )
                        );
        return toSummary(billing);
    }

    public List<Payment> getPayments(Long billingId) {
        adminSession.requireCurrentUser();
        return paymentRepository.findByBillingId(billingId);
    }

    public Payment processPayment(
            Long reservationId,
            double amount,
            PaymentMethod method,
            PaymentType requestedType,
            String note
    ) {
        adminSession.requireCurrentUser();
        validatePositiveAmount(amount);
        if (method == null) {
            throw new IllegalArgumentException(
                    "Select a payment method."
            );
        }

        try {
            Payment payment = JpaUtil.executeInTransaction(
                    () -> {
                        Billing billing = requireBilling(
                                reservationId
                        );
                        Reservation reservation =
                                billing.getReservation();

                        if (reservation.getStatus()
                                == ReservationStatus.CANCELLED
                                || reservation.getStatus()
                                == ReservationStatus.CHECKED_OUT) {
                            throw new IllegalStateException(
                                    "Payments cannot be added to a cancelled "
                                            + "or checked-out reservation."
                            );
                        }

                        double outstanding =
                                calculateOutstanding(billing);
                        if (amount - outstanding > 0.009) {
                            throw new IllegalArgumentException(
                                    String.format(
                                            "Payment exceeds the outstanding "
                                                    + "balance of CAD %.2f.",
                                            outstanding
                                    )
                            );
                        }

                        PaymentType paymentType =
                                determinePaymentType(
                                        requestedType,
                                        amount,
                                        outstanding,
                                        method
                                );
                        double recordedAmount =
                                roundMoney(amount);

                        if (method
                                == PaymentMethod.LOYALTY_POINTS) {
                            LoyaltyService.Redemption redemption =
                                    loyaltyService.redeem(
                                            billing,
                                            amount
                                    );

                            BillingCalculationStrategy strategy =
                                    new LoyaltyBillingStrategy(
                                            redemption.amount()
                                    );
                            double payableBefore =
                                    billing.getAmountAfterDiscount();
                            double payableAfterRedemption =
                                    strategy.calculate(
                                            payableBefore
                                    );
                            double amountToRecord = roundMoney(
                                    payableBefore
                                            - payableAfterRedemption
                            );
                            recordedAmount = amountToRecord;
                            paymentType =
                                    PaymentType.LOYALTY_REDEMPTION;
                        }

                        Payment saved =
                                paymentRepository.save(
                                        new Payment(
                                                billing,
                                                recordedAmount,
                                                method,
                                                paymentType,
                                                normalizeNote(note)
                                        )
                                );

                        if (method != PaymentMethod.LOYALTY_POINTS) {
                            loyaltyService.earnPoints(
                                    reservation,
                                    recordedAmount
                            );
                        }

                        return saved;
                    }
            );

            AppLogger.audit(
                    LOGGER,
                    Level.INFO,
                    adminSession.getActorName(),
                    "PAYMENT_PROCESSED",
                    "Reservation",
                    String.valueOf(reservationId),
                    String.format(
                            "CAD %.2f processed using %s as %s.",
                            payment.getAmount(),
                            payment.getPaymentMethod(),
                            payment.getPaymentType()
                    )
            );
            return payment;

        } catch (RuntimeException exception) {
            AppLogger.exception(
                    LOGGER,
                    "Payment processing failed.",
                    exception
            );
            throw exception;
        }
    }

    public Payment refund(
            Long reservationId,
            double amount,
            PaymentMethod method,
            String note
    ) {
        adminSession.requireCurrentUser();
        validatePositiveAmount(amount);
        if (method == null
                || method == PaymentMethod.LOYALTY_POINTS) {
            throw new IllegalArgumentException(
                    "Refunds must use cash or card."
            );
        }

        if (adminSession.requireCurrentUser().getRole()
                == AdminRole.ADMIN
                && amount > 500) {
            throw new SecurityException(
                    "Refunds above CAD 500 require a Manager."
            );
        }

        Payment refund = JpaUtil.executeInTransaction(
                () -> {
                    Billing billing = requireBilling(
                            reservationId
                    );
                    double paid = paymentRepository.getNetPaid(
                            billing.getBillingId()
                    );
                    if (amount - paid > 0.009) {
                        throw new IllegalArgumentException(
                                String.format(
                                        "Refund cannot exceed net payments "
                                                + "of CAD %.2f.",
                                        paid
                                )
                        );
                    }

                    Payment saved = paymentRepository.save(
                            new Payment(
                                    billing,
                                    -roundMoney(amount),
                                    method,
                                    PaymentType.REFUND,
                                    normalizeNote(note)
                            )
                    );

                    loyaltyService.reversePointsForRefund(
                            billing.getReservation(),
                            amount
                    );
                    return saved;
                }
        );

        AppLogger.audit(
                LOGGER,
                Level.WARNING,
                adminSession.getActorName(),
                "REFUND_PROCESSED",
                "Reservation",
                String.valueOf(reservationId),
                String.format(
                        "Refunded CAD %.2f using %s.",
                        amount,
                        method
                )
        );
        return refund;
    }

    public Reservation checkIn(Long reservationId) {
        adminSession.requireCurrentUser();

        Reservation result = JpaUtil.executeInTransaction(
                () -> {
                    Reservation reservation =
                            requireReservation(reservationId);
                    if (reservation.getStatus()
                            != ReservationStatus.CONFIRMED) {
                        throw new IllegalStateException(
                                "Only confirmed reservations can be checked in."
                        );
                    }

                    reservation.setStatus(
                            ReservationStatus.CHECKED_IN
                    );
                    for (Room room : assignedRooms(reservation)) {
                        room.setStatus(RoomStatus.OCCUPIED);
                        roomRepository.update(room);
                    }
                    return reservationRepository.update(
                            reservation
                    );
                }
        );

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                adminSession.getActorName(),
                "CHECK_IN",
                "Reservation",
                String.valueOf(reservationId),
                "Guest checked in and assigned rooms were marked occupied."
        );
        return result;
    }

    public Reservation checkout(Long reservationId) {
        adminSession.requireCurrentUser();

        CheckoutResult checkoutResult =
                JpaUtil.executeInTransaction(
                        () -> {
                            Billing billing = requireBilling(
                                    reservationId
                            );
                            Reservation reservation =
                                    requireReservation(
                                            reservationId
                                    );

                            if (reservation.getStatus()
                                    != ReservationStatus.CHECKED_IN) {
                                throw new IllegalStateException(
                                        "Only checked-in reservations "
                                                + "can be checked out."
                                );
                            }

                            double outstanding =
                                    calculateOutstanding(billing);
                            if (outstanding > 0.009) {
                                throw new IllegalStateException(
                                        String.format(
                                                "Checkout is blocked. "
                                                        + "Outstanding balance: "
                                                        + "CAD %.2f.",
                                                outstanding
                                        )
                                );
                            }

                            reservation.setStatus(
                                    ReservationStatus.CHECKED_OUT
                            );
                            Reservation updated =
                                    reservationRepository.update(
                                            reservation
                                    );

                            List<Room> rooms =
                                    assignedRooms(reservation);
                            for (Room room : rooms) {
                                room.setStatus(RoomStatus.AVAILABLE);
                                roomRepository.update(room);
                            }

                            notificationRepository.save(
                                    new AdminNotification(
                                            NotificationType.CHECKOUT,
                                            adminSession.getActorName(),
                                            "Checkout completed for RES-"
                                                    + reservationId
                                                    + ". Invite "
                                                    + reservation.getGuest()
                                                    .getFullName()
                                                    + " to submit kiosk feedback.",
                                            null,
                                            null
                                    )
                            );

                            return new CheckoutResult(
                                    updated,
                                    rooms
                            );
                        }
                );

        for (Room room : checkoutResult.rooms()) {
            publisher.roomBecameAvailable(
                    room,
                    LocalDate.now()
            );
        }

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                adminSession.getActorName(),
                "CHECKOUT",
                "Reservation",
                String.valueOf(reservationId),
                "Final balance settled, checkout completed, "
                        + "and rooms marked available."
        );

        return checkoutResult.reservation();
    }

    public double calculateOutstanding(Billing billing) {
        double payable = calculatePayable(billing);
        double paid = paymentRepository.getNetPaid(
                billing.getBillingId()
        );
        return roundMoney(Math.max(0, payable - paid));
    }

    public double calculatePayable(Billing billing) {
        BillingCalculationStrategy strategy =
                new StandardBillingStrategy();
        double amount = strategy.calculate(
                billing.getTotalAmount()
        );

        if (billing.getDiscountPercentage() > 0) {
            strategy = new DiscountBillingStrategy(
                    billing.getDiscountPercentage()
            );
            amount = strategy.calculate(
                    billing.getTotalAmount()
            );
        }

        return roundMoney(amount);
    }

    private BillingSummary toSummary(Billing billing) {
        double paid = roundMoney(
                paymentRepository.getNetPaid(
                        billing.getBillingId()
                )
        );
        double payable = calculatePayable(billing);
        return new BillingSummary(
                billing,
                payable,
                paid,
                roundMoney(Math.max(0, payable - paid))
        );
    }

    private Billing requireBilling(Long reservationId) {
        return billingRepository
                .findByReservationIdWithGuest(reservationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Billing record was not found."
                        )
                );
    }

    private Reservation requireReservation(
            Long reservationId
    ) {
        return reservationRepository
                .findByIdWithDetails(reservationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Reservation was not found."
                        )
                );
    }

    private List<Room> assignedRooms(
            Reservation reservation
    ) {
        Set<Room> rooms = new LinkedHashSet<>();
        for (ReservationRoom assignment :
                reservation.getReservationRooms()) {
            if (assignment.getRoom() != null) {
                rooms.add(assignment.getRoom());
            }
        }
        if (rooms.isEmpty() && reservation.getRoom() != null) {
            rooms.add(reservation.getRoom());
        }
        return new ArrayList<>(rooms);
    }

    private PaymentType determinePaymentType(
            PaymentType requestedType,
            double amount,
            double outstanding,
            PaymentMethod method
    ) {
        if (method == PaymentMethod.LOYALTY_POINTS) {
            return PaymentType.LOYALTY_REDEMPTION;
        }
        if (Math.abs(amount - outstanding) < 0.009) {
            return PaymentType.FINAL_PAYMENT;
        }
        if (requestedType == PaymentType.DEPOSIT) {
            return PaymentType.DEPOSIT;
        }
        return PaymentType.PARTIAL_PAYMENT;
    }

    private void validatePositiveAmount(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero."
            );
        }
    }

    private String normalizeNote(String note) {
        String value = note == null ? "" : note.trim();
        if (value.length() > 255) {
            throw new IllegalArgumentException(
                    "Payment note cannot exceed 255 characters."
            );
        }
        return value.isBlank()
                ? "Front desk transaction"
                : value;
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record BillingSummary(
            Billing billing,
            double payable,
            double paid,
            double outstanding
    ) {
    }

    private record CheckoutResult(
            Reservation reservation,
            List<Room> rooms
    ) {
    }
}
