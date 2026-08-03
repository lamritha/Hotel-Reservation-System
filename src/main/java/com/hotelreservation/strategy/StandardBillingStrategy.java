package com.hotelreservation.strategy;

public final class StandardBillingStrategy
        implements BillingCalculationStrategy {

    @Override
    public double calculate(double amount) {
        return Math.max(0, amount);
    }
}
