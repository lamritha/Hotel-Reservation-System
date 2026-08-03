package com.hotelreservation.service;

import com.hotelreservation.model.Billing;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.ReservationStatus;
import com.hotelreservation.model.RoomStatus;
import com.hotelreservation.repository.BillingRepository;
import com.hotelreservation.repository.GuestRepository;
import com.hotelreservation.repository.PaymentRepository;
import com.hotelreservation.repository.ReservationRepository;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.repository.WaitlistRepository;
import com.hotelreservation.security.AdminSession;

import java.time.LocalDate;
import java.util.List;

public class DashboardService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final BillingRepository billingRepository;
    private final PaymentRepository paymentRepository;
    private final WaitlistRepository waitlistRepository;
    private final NotificationService notificationService;
    private final AdminSession adminSession;

    public DashboardService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            GuestRepository guestRepository,
            BillingRepository billingRepository,
            PaymentRepository paymentRepository,
            WaitlistRepository waitlistRepository,
            NotificationService notificationService,
            AdminSession adminSession
    ) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
        this.billingRepository = billingRepository;
        this.paymentRepository = paymentRepository;
        this.waitlistRepository = waitlistRepository;
        this.notificationService = notificationService;
        this.adminSession = adminSession;
    }

    public DashboardSummary getSummary() {
        adminSession.requireCurrentUser();

        long outstandingBills = 0;
        for (Billing billing :
                billingRepository.search("")) {
            double netPaid = paymentRepository.getNetPaid(
                    billing.getBillingId()
            );
            if (billing.getAmountAfterDiscount()
                    - netPaid > 0.009) {
                outstandingBills++;
            }
        }

        List<Reservation> recent =
                reservationRepository.findAll();
        if (recent.size() > 5) {
            recent = recent.subList(0, 5);
        }

        return new DashboardSummary(
                adminSession.requireCurrentUser()
                        .getFullName(),
                adminSession.requireCurrentUser().getRole()
                        .name(),
                reservationRepository.countAll(),
                reservationRepository.countByStatusValue(
                        ReservationStatus.CHECKED_IN
                ),
                roomRepository.countByStatus(
                        RoomStatus.AVAILABLE
                ),
                roomRepository.countByStatus(
                        RoomStatus.OCCUPIED
                ),
                guestRepository.countAll(),
                outstandingBills,
                waitlistRepository.countActive(),
                notificationService.countUnread(),
                paymentRepository.sumPaymentsForDate(
                        LocalDate.now()
                ),
                recent
        );
    }

    public record DashboardSummary(
            String adminName,
            String role,
            long totalReservations,
            long checkedInReservations,
            long availableRooms,
            long occupiedRooms,
            long totalGuests,
            long outstandingBills,
            long activeWaitlist,
            long unreadNotifications,
            double revenueToday,
            List<Reservation> recentReservations
    ) {
    }
}
