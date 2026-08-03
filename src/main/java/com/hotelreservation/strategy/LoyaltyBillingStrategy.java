package com.hotelreservation.strategy;

public final class LoyaltyBillingStrategy
        implements BillingCalculationStrategy {

    private final double redemptionAmount;

    public LoyaltyBillingStrategy(double redemptionAmount) {
        if (redemptionAmount < 0) {
            throw new IllegalArgumentException(
                    "Loyalty redemption cannot be negative."
            );
        }
        this.redemptionAmount = redemptionAmount;
    }

    @Override
    public double calculate(double amount) {
        return Math.max(0, amount - redemptionAmount);
    }
}
