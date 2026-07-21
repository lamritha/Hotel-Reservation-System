package com.hotelreservation.util;

import com.hotelreservation.entity.RoomType;

import java.time.LocalDate;

public class BookingSession {

    private static String firstName = "";
    private static String lastName = "";
    private static String email = "";
    private static String phone = "";
    private static String address = "";

    private static int numAdults = 2;
    private static int numChildren = 0;

    private static LocalDate checkInDate = LocalDate.of(2026, 7, 17);
    private static LocalDate checkOutDate = LocalDate.of(2026, 7, 20);

    private static boolean groupBooking = false;

    private static RoomType selectedRoomType = null;
    private static String selectedRoomName = "Not selected";

    private static int singleRoomQuantity = 0;
    private static int doubleRoomQuantity = 0;
    private static int deluxeRoomQuantity = 0;
    private static int penthouseRoomQuantity = 0;

    private static boolean wifiSelected;
    private static boolean breakfastSelected;
    private static boolean spaSelected;
    private static boolean parkingSelected;
    private static boolean laundrySelected;
    private static boolean airportPickupSelected;

    private static boolean loyaltyEnrolled;
    private static String loyaltyNumber = "";
    private static int loyaltyPointsBalance;

    private static Long savedReservationId;
    private static Long savedGuestId;
    private static Long savedRoomId;

    private BookingSession() {
        // Prevent object creation
    }

    /**
     * Restores all session fields to their initial default values.
     */
    public static void reset() {
        firstName = "";
        lastName = "";
        email = "";
        phone = "";
        address = "";

        numAdults = 2;
        numChildren = 0;

        checkInDate = LocalDate.of(2026, 7, 17);
        checkOutDate = LocalDate.of(2026, 7, 20);

        groupBooking = false;

        selectedRoomType = null;
        selectedRoomName = "Not selected";

        singleRoomQuantity = 0;
        doubleRoomQuantity = 0;
        deluxeRoomQuantity = 0;
        penthouseRoomQuantity = 0;

        wifiSelected = false;
        breakfastSelected = false;
        spaSelected = false;
        parkingSelected = false;
        laundrySelected = false;
        airportPickupSelected = false;

        loyaltyEnrolled = false;
        loyaltyNumber = "";
        loyaltyPointsBalance = 0;

        savedReservationId = null;
        savedGuestId = null;
        savedRoomId = null;
    }

    public static String getFirstName() {
        return firstName;
    }

    public static void setFirstName(String firstName) {
        BookingSession.firstName = firstName;
    }

    public static String getLastName() {
        return lastName;
    }

    public static void setLastName(String lastName) {
        BookingSession.lastName = lastName;
    }

    public static String getEmail() {
        return email;
    }

    public static void setEmail(String email) {
        BookingSession.email = email;
    }

    public static String getPhone() {
        return phone;
    }

    public static void setPhone(String phone) {
        BookingSession.phone = phone;
    }

    public static String getAddress() {
        return address;
    }

    public static void setAddress(String address) {
        BookingSession.address = address;
    }

    public static int getNumAdults() {
        return numAdults;
    }

    public static void setNumAdults(int numAdults) {
        BookingSession.numAdults = numAdults;
    }

    public static int getNumChildren() {
        return numChildren;
    }

    public static void setNumChildren(int numChildren) {
        BookingSession.numChildren = numChildren;
    }

    public static int getTotalGuests() {
        return numAdults + numChildren;
    }

    public static LocalDate getCheckInDate() {
        return checkInDate;
    }

    public static void setCheckInDate(LocalDate checkInDate) {
        BookingSession.checkInDate = checkInDate;
    }

    public static LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public static void setCheckOutDate(LocalDate checkOutDate) {
        BookingSession.checkOutDate = checkOutDate;
    }

    public static boolean isGroupBooking() {
        return groupBooking;
    }

    public static void setGroupBooking(boolean groupBooking) {
        BookingSession.groupBooking = groupBooking;
    }

    public static RoomType getSelectedRoomType() {
        return selectedRoomType;
    }

    public static void setSelectedRoomType(RoomType roomType) {
        selectedRoomType = roomType;

        if (roomType == null) {
            selectedRoomName = "Not selected";
            return;
        }

        switch (roomType) {
            case SINGLE -> selectedRoomName = "Single Room";
            case DOUBLE -> selectedRoomName = "Double Room";
            case DELUXE -> selectedRoomName = "Deluxe Room";
            case PENTHOUSE -> selectedRoomName = "Penthouse";
        }
    }

    public static String getSelectedRoomName() {
        return selectedRoomName;
    }

    public static int getSingleRoomQuantity() {
        return singleRoomQuantity;
    }

    public static void setSingleRoomQuantity(int singleRoomQuantity) {
        BookingSession.singleRoomQuantity = Math.max(0, singleRoomQuantity);
    }

    public static int getDoubleRoomQuantity() {
        return doubleRoomQuantity;
    }

    public static void setDoubleRoomQuantity(int doubleRoomQuantity) {
        BookingSession.doubleRoomQuantity = Math.max(0, doubleRoomQuantity);
    }

    public static int getDeluxeRoomQuantity() {
        return deluxeRoomQuantity;
    }

    public static void setDeluxeRoomQuantity(int deluxeRoomQuantity) {
        BookingSession.deluxeRoomQuantity = Math.max(0, deluxeRoomQuantity);
    }

    public static int getPenthouseRoomQuantity() {
        return penthouseRoomQuantity;
    }

    public static void setPenthouseRoomQuantity(int penthouseRoomQuantity) {
        BookingSession.penthouseRoomQuantity = Math.max(0, penthouseRoomQuantity);
    }

    public static int getTotalRoomQuantity() {
        return singleRoomQuantity + doubleRoomQuantity + deluxeRoomQuantity + penthouseRoomQuantity;
    }

    public static int getTotalRoomCapacity() {
        return (singleRoomQuantity * 2)
                + (doubleRoomQuantity * 4)
                + (deluxeRoomQuantity * 2)
                + (penthouseRoomQuantity * 2);
    }

    public static String getRoomPlanSummary() {
        StringBuilder summary = new StringBuilder();

        if (singleRoomQuantity > 0) {
            summary.append(singleRoomQuantity).append(" Single Room(s) ");
        }

        if (doubleRoomQuantity > 0) {
            summary.append(doubleRoomQuantity).append(" Double Room(s) ");
        }

        if (deluxeRoomQuantity > 0) {
            summary.append(deluxeRoomQuantity).append(" Deluxe Room(s) ");
        }

        if (penthouseRoomQuantity > 0) {
            summary.append(penthouseRoomQuantity).append(" Penthouse Room(s) ");
        }

        if (summary.isEmpty()) {
            return "No rooms selected";
        }

        return summary.toString().trim();
    }

    public static Long getSavedReservationId() {
        return savedReservationId;
    }

    public static void setSavedReservationId(Long savedReservationId) {
        BookingSession.savedReservationId = savedReservationId;
    }

    public static Long getSavedGuestId() {
        return savedGuestId;
    }

    public static void setSavedGuestId(Long savedGuestId) {
        BookingSession.savedGuestId = savedGuestId;
    }

    public static Long getSavedRoomId() {
        return savedRoomId;
    }

    public static void setSavedRoomId(Long savedRoomId) {
        BookingSession.savedRoomId = savedRoomId;
    }

    public static boolean isLoyaltyEnrolled() {
        return loyaltyEnrolled;
    }

    public static void setLoyaltyEnrolled(boolean loyaltyEnrolled) {
        BookingSession.loyaltyEnrolled = loyaltyEnrolled;
    }

    public static String getLoyaltyNumber() {
        return loyaltyNumber;
    }

    public static void setLoyaltyNumber(String loyaltyNumber) {
        BookingSession.loyaltyNumber = loyaltyNumber;
    }

    public static int getLoyaltyPointsBalance() {
        return loyaltyPointsBalance;
    }

    public static void setLoyaltyPointsBalance(int loyaltyPointsBalance) {
        BookingSession.loyaltyPointsBalance = loyaltyPointsBalance;
    }

    public static boolean isWifiSelected() {
        return wifiSelected;
    }

    public static void setWifiSelected(boolean wifiSelected) {
        BookingSession.wifiSelected = wifiSelected;
    }

    public static boolean isBreakfastSelected() {
        return breakfastSelected;
    }

    public static void setBreakfastSelected(boolean breakfastSelected) {
        BookingSession.breakfastSelected = breakfastSelected;
    }

    public static boolean isSpaSelected() {
        return spaSelected;
    }

    public static void setSpaSelected(boolean spaSelected) {
        BookingSession.spaSelected = spaSelected;
    }

    public static boolean isParkingSelected() {
        return parkingSelected;
    }

    public static void setParkingSelected(boolean parkingSelected) {
        BookingSession.parkingSelected = parkingSelected;
    }

    public static boolean isLaundrySelected() {
        return laundrySelected;
    }

    public static void setLaundrySelected(boolean laundrySelected) {
        BookingSession.laundrySelected = laundrySelected;
    }

    public static boolean isAirportPickupSelected() {
        return airportPickupSelected;
    }

    public static void setAirportPickupSelected(boolean airportPickupSelected) {
        BookingSession.airportPickupSelected = airportPickupSelected;
    }
}