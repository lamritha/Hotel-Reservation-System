package com.hotelreservation.service;

import com.hotelreservation.config.HotelPolicyConfig;
import com.hotelreservation.model.Billing;
import com.hotelreservation.model.Guest;
import com.hotelreservation.model.LoyaltyAccount;
import com.hotelreservation.model.LoyaltyTransaction;
import com.hotelreservation.model.LoyaltyTransactionType;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.repository.LoyaltyAccountRepository;
import com.hotelreservation.repository.LoyaltyTransactionRepository;
import com.hotelreservation.util.AppLogger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoyaltyService {

    private static final Logger LOGGER =
            AppLogger.getLogger(LoyaltyService.class);

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository
            transactionRepository;

    public LoyaltyService(
            LoyaltyAccountRepository accountRepository,
            LoyaltyTransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public LoyaltyAccount enrollGuest(
            Guest guest,
            String actor
    ) {
        if (guest == null || guest.getGuestId() == null) {
            throw new IllegalArgumentException(
                    "A saved guest is required for loyalty enrollment."
            );
        }

        Optional<LoyaltyAccount> existing =
                accountRepository.findByGuestId(
                        guest.getGuestId()
                );
        if (existing.isPresent()) {
            return existing.get();
        }

        String loyaltyNumber = "LOY-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        LoyaltyAccount account = accountRepository.save(
                new LoyaltyAccount(
                        guest,
                        loyaltyNumber,
                        0,
                        0,
                        0
                )
        );

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                actor,
                "LOYALTY_ENROLLED",
                "LoyaltyAccount",
                account.getLoyaltyNumber(),
                "Loyalty account created for "
                        + guest.getFullName() + "."
        );

        return account;
    }

    public int earnPoints(
            Reservation reservation,
            double paidAmount
    ) {
        if (reservation == null
                || reservation.getGuest() == null
                || paidAmount <= 0) {
            return 0;
        }

        Optional<LoyaltyAccount> optionalAccount =
                accountRepository.findByGuestId(
                        reservation.getGuest().getGuestId()
                );
        if (optionalAccount.isEmpty()) {
            return 0;
        }

        int points = (int) Math.floor(
                paidAmount
                        / HotelPolicyConfig
                        .LOYALTY_EARNING_DOLLARS_PER_POINT
        );
        if (points <= 0) {
            return 0;
        }

        LoyaltyAccount account = optionalAccount.get();
        account.setPointsBalance(
                account.getPointsBalance() + points
        );
        account.setTotalEarned(
                account.getTotalEarned() + points
        );
        accountRepository.update(account);

        transactionRepository.save(
                new LoyaltyTransaction(
                        account,
                        reservation,
                        LoyaltyTransactionType.EARN,
                        points,
                        paidAmount,
                        "Points earned from payment."
                )
        );

        return points;
    }

    public Redemption redeem(
            Billing billing,
            double requestedDollars
    ) {
        if (billing == null
                || billing.getReservation() == null) {
            throw new IllegalArgumentException(
                    "A valid bill is required."
            );
        }
        if (requestedDollars <= 0) {
            throw new IllegalArgumentException(
                    "Loyalty redemption must be greater than zero."
            );
        }

        LoyaltyAccount account =
                accountRepository.findByGuestId(
                                billing.getReservation()
                                        .getGuest()
                                        .getGuestId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "This guest is not enrolled in loyalty."
                                )
                        );

        double maximumByPolicy =
                billing.getAmountAfterDiscount()
                        * HotelPolicyConfig
                        .LOYALTY_REDEMPTION_CAP;
        double availableDollars =
                account.getPointsBalance()
                        * HotelPolicyConfig
                        .LOYALTY_DOLLARS_PER_POINT;

        double redeemable = roundMoney(
                Math.min(maximumByPolicy, availableDollars)
        );
        if (requestedDollars - redeemable > 0.009) {
            throw new IllegalArgumentException(
                    String.format(
                            "Maximum loyalty redemption is CAD %.2f.",
                            redeemable
                    )
            );
        }

        int points = (int) Math.ceil(
                requestedDollars
                        / HotelPolicyConfig
                        .LOYALTY_DOLLARS_PER_POINT
        );
        if (points > account.getPointsBalance()) {
            throw new IllegalArgumentException(
                    "The loyalty account does not have enough points."
            );
        }

        account.setPointsBalance(
                account.getPointsBalance() - points
        );
        account.setTotalRedeemed(
                account.getTotalRedeemed() + points
        );
        accountRepository.update(account);

        transactionRepository.save(
                new LoyaltyTransaction(
                        account,
                        billing.getReservation(),
                        LoyaltyTransactionType.REDEEM,
                        -points,
                        requestedDollars,
                        "Points redeemed toward reservation balance."
                )
        );

        return new Redemption(
                points,
                roundMoney(requestedDollars)
        );
    }

    public void reversePointsForRefund(
            Reservation reservation,
            double refundAmount
    ) {
        if (reservation == null || refundAmount <= 0) {
            return;
        }

        accountRepository.findByGuestId(
                reservation.getGuest().getGuestId()
        ).ifPresent(account -> {
            int points = (int) Math.floor(
                    refundAmount
                            / HotelPolicyConfig
                            .LOYALTY_EARNING_DOLLARS_PER_POINT
            );
            points = Math.min(points, account.getPointsBalance());
            if (points <= 0) {
                return;
            }

            account.setPointsBalance(
                    account.getPointsBalance() - points
            );
            accountRepository.update(account);

            transactionRepository.save(
                    new LoyaltyTransaction(
                            account,
                            reservation,
                            LoyaltyTransactionType.ADJUSTMENT,
                            -points,
                            refundAmount,
                            "Points reversed because of a refund."
                    )
            );
        });
    }

    public List<LoyaltyAccount> searchAccounts(
            String keyword
    ) {
        return accountRepository.search(keyword);
    }

    public List<LoyaltyTransaction> getHistory(
            Long accountId
    ) {
        return transactionRepository.findByAccountId(
                accountId
        );
    }

    public Optional<LoyaltyAccount> findByPhone(
            String phone
    ) {
        return accountRepository.findByGuestPhone(phone);
    }

    public Optional<LoyaltyAccount> findByGuestId(
            Long guestId
    ) {
        return accountRepository.findByGuestId(guestId);
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record Redemption(
            int points,
            double amount
    ) {
    }
}
