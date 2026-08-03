package com.hotelreservation.service;

import com.hotelreservation.config.HotelPolicyConfig;
import com.hotelreservation.model.AdminUser;
import com.hotelreservation.model.Billing;
import com.hotelreservation.model.Discount;
import com.hotelreservation.model.ReservationStatus;
import com.hotelreservation.repository.BillingRepository;
import com.hotelreservation.repository.DiscountRepository;
import com.hotelreservation.repository.PaymentRepository;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.strategy.DiscountBillingStrategy;
import com.hotelreservation.util.AppLogger;
import com.hotelreservation.util.JpaUtil;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscountService {

    private static final Logger LOGGER =
            AppLogger.getLogger(DiscountService.class);

    private final BillingRepository billingRepository;
    private final DiscountRepository discountRepository;
    private final PaymentRepository paymentRepository;
    private final AdminSession adminSession;

    public DiscountService(
            BillingRepository billingRepository,
            DiscountRepository discountRepository,
            PaymentRepository paymentRepository,
            AdminSession adminSession
    ) {
        this.billingRepository = billingRepository;
        this.discountRepository = discountRepository;
        this.paymentRepository = paymentRepository;
        this.adminSession = adminSession;
    }

    public List<Billing> searchBills(String keyword) {
        adminSession.requireCurrentUser();
        return billingRepository.search(keyword);
    }

    public double getCurrentRoleCapPercent() {
        AdminUser admin = adminSession.requireCurrentUser();
        return HotelPolicyConfig.discountCap(
                admin.getRole()
        ) * 100.0;
    }

    public Discount applyDiscount(
            Long reservationId,
            double percentage,
            String reason
    ) {
        AdminUser admin = adminSession.requireCurrentUser();
        String normalizedReason =
                reason == null ? "" : reason.trim();

        if (!Double.isFinite(percentage)
                || percentage < 0) {
            throw new IllegalArgumentException(
                    "Discount percentage cannot be negative."
            );
        }
        if (normalizedReason.isBlank()) {
            throw new IllegalArgumentException(
                    "Enter a reason for the discount."
            );
        }
        if (normalizedReason.length() > 255) {
            throw new IllegalArgumentException(
                    "Discount reason cannot exceed 255 characters."
            );
        }

        double rate = percentage / 100.0;
        double cap = HotelPolicyConfig.discountCap(
                admin.getRole()
        );
        if (rate - cap > 0.00001) {
            throw new SecurityException(
                    String.format(
                            "%s discount cap is %.0f%%.",
                            admin.getRole(),
                            cap * 100
                    )
            );
        }

        Discount discount = JpaUtil.executeInTransaction(
                () -> {
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

                    ReservationStatus status =
                            billing.getReservation().getStatus();
                    if (status == ReservationStatus.CANCELLED
                            || status
                            == ReservationStatus.CHECKED_OUT) {
                        throw new IllegalStateException(
                                "Discounts cannot be changed for a "
                                        + "cancelled or checked-out reservation."
                        );
                    }

                    double finalAmount =
                            new DiscountBillingStrategy(rate)
                                    .calculate(
                                            billing.getTotalAmount()
                                    );
                    double paid = paymentRepository.getNetPaid(
                            billing.getBillingId()
                    );
                    if (paid - finalAmount > 0.009) {
                        throw new IllegalStateException(
                                "This discount would create a negative "
                                        + "balance. Process a refund first."
                        );
                    }

                    double amount = roundMoney(
                            billing.getTotalAmount()
                                    - finalAmount
                    );
                    billing.setDiscountPercentage(rate);
                    billing.setDiscountAmount(amount);
                    billing.setDiscountAppliedBy(
                            admin.getUsername()
                    );
                    billingRepository.update(billing);

                    return discountRepository.save(
                            new Discount(
                                    billing,
                                    percentage,
                                    amount,
                                    normalizedReason,
                                    admin.getUsername(),
                                    admin.getRole()
                            )
                    );
                }
        );

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                admin.getUsername(),
                "DISCOUNT_APPLIED",
                "Reservation",
                String.valueOf(reservationId),
                String.format(
                        "%.2f%% discount (CAD %.2f) applied. Reason: %s",
                        percentage,
                        discount.getAmount(),
                        normalizedReason
                )
        );

        return discount;
    }

    public List<Discount> getHistory(Long billingId) {
        adminSession.requireCurrentUser();
        return discountRepository.findByBillingId(billingId);
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
