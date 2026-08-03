package com.hotelreservation.config;

import com.hotelreservation.model.AdminRole;

import java.time.LocalDate;
import java.time.MonthDay;

/**
 * Central location for business values that the hotel can tune without
 * changing controllers or persistence code.
 */
public final class HotelPolicyConfig {

    public static final double TAX_RATE = 0.13;
    public static final double WEEKDAY_MULTIPLIER = 1.00;
    public static final double WEEKEND_MULTIPLIER = 1.25;
    public static final double PEAK_SEASON_MULTIPLIER = 1.15;

    public static final double ADMIN_DISCOUNT_CAP = 0.15;
    public static final double MANAGER_DISCOUNT_CAP = 0.30;

    public static final double LOYALTY_DOLLARS_PER_POINT = 0.01;
    public static final double LOYALTY_EARNING_DOLLARS_PER_POINT = 10.00;
    public static final double LOYALTY_REDEMPTION_CAP = 0.25;

    public static final int FEEDBACK_COMMENT_MAX_LENGTH = 1000;

    public static final double WIFI_PRICE = 15.00;
    public static final double BREAKFAST_PRICE_PER_NIGHT = 20.00;
    public static final double PARKING_PRICE_PER_NIGHT = 25.00;
    public static final double SPA_PRICE = 80.00;
    public static final double LAUNDRY_PRICE = 30.00;
    public static final double AIRPORT_PICKUP_PRICE = 60.00;

    private HotelPolicyConfig() {
    }

    public static double discountCap(AdminRole role) {
        if (role == AdminRole.MANAGER) {
            return MANAGER_DISCOUNT_CAP;
        }
        return ADMIN_DISCOUNT_CAP;
    }

    public static double seasonalMultiplier(LocalDate date) {
        MonthDay value = MonthDay.from(date);
        MonthDay summerStart = MonthDay.of(6, 1);
        MonthDay summerEnd = MonthDay.of(8, 31);
        MonthDay holidayStart = MonthDay.of(12, 20);
        MonthDay holidayEnd = MonthDay.of(1, 5);

        boolean summerPeak = value.compareTo(summerStart) >= 0
                && value.compareTo(summerEnd) <= 0;
        boolean holidayPeak = value.compareTo(holidayStart) >= 0
                || value.compareTo(holidayEnd) <= 0;

        return summerPeak || holidayPeak
                ? PEAK_SEASON_MULTIPLIER
                : 1.00;
    }
}
