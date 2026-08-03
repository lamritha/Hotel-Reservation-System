package com.hotelreservation.strategy;

public final class DiscountBillingStrategy
        implements BillingCalculationStrategy {

    private final double discountRate;

    public DiscountBillingStrategy(double discountRate) {
        if (discountRate < 0 || discountRate > 1) {
            throw new IllegalArgumentException(
                    "Discount rate must be between 0 and 1."
            );
        }
        this.discountRate = discountRate;
    }

    @Override
    public double calculate(double amount) {
        return Math.max(0, amount * (1 - discountRate));
    }
}
