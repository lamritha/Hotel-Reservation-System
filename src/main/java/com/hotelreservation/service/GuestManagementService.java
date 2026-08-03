package com.hotelreservation.service;

import com.hotelreservation.model.Guest;
import com.hotelreservation.model.LoyaltyAccount;
import com.hotelreservation.repository.GuestRepository;
import com.hotelreservation.security.AdminSession;

import java.util.List;

public class GuestManagementService {

    private final GuestRepository guestRepository;
    private final LoyaltyService loyaltyService;
    private final AdminSession adminSession;

    public GuestManagementService(
            GuestRepository guestRepository,
            LoyaltyService loyaltyService,
            AdminSession adminSession
    ) {
        this.guestRepository = guestRepository;
        this.loyaltyService = loyaltyService;
        this.adminSession = adminSession;
    }

    public List<Guest> search(String keyword) {
        adminSession.requireCurrentUser();
        return guestRepository.search(keyword);
    }

    public LoyaltyAccount enrollInLoyalty(Guest guest) {
        adminSession.requireCurrentUser();
        if (guest == null) {
            throw new IllegalArgumentException(
                    "Select a guest first."
            );
        }
        return loyaltyService.enrollGuest(
                guest,
                adminSession.getActorName()
        );
    }
}
