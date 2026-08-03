package com.hotelreservation.config;

import com.hotelreservation.app.AdminAccountSeeder;
import com.hotelreservation.app.ApplicationDataSeeder;
import com.hotelreservation.controller.admin.AdminDashboardController;
import com.hotelreservation.controller.admin.AdminLoginController;
import com.hotelreservation.controller.admin.AdminNavigationController;
import com.hotelreservation.controller.admin.BillingPaymentController;
import com.hotelreservation.controller.admin.DiscountManagementController;
import com.hotelreservation.controller.admin.FeedbackManagementController;
import com.hotelreservation.controller.admin.GuestManagementController;
import com.hotelreservation.controller.admin.LoyaltyManagementController;
import com.hotelreservation.controller.admin.NotificationsController;
import com.hotelreservation.controller.admin.ReportsController;
import com.hotelreservation.controller.admin.ReservationFormController;
import com.hotelreservation.controller.admin.ReservationManagementController;
import com.hotelreservation.controller.admin.RoomManagementController;
import com.hotelreservation.controller.admin.WaitlistController;
import com.hotelreservation.controller.kiosk.AddOnsController;
import com.hotelreservation.controller.kiosk.BookingSummaryController;
import com.hotelreservation.controller.kiosk.FeedbackController;
import com.hotelreservation.controller.kiosk.FeedbackLookupController;
import com.hotelreservation.controller.kiosk.LoyaltyCheckController;
import com.hotelreservation.controller.kiosk.OccupancyController;
import com.hotelreservation.controller.kiosk.RoomSelectionController;
import com.hotelreservation.events.AdminNotificationObserver;
import com.hotelreservation.events.AvailabilityEventPublisher;
import com.hotelreservation.repository.AddOnRepository;
import com.hotelreservation.repository.AdminUserRepository;
import com.hotelreservation.repository.BillingRepository;
import com.hotelreservation.repository.DiscountRepository;
import com.hotelreservation.repository.FeedbackRepository;
import com.hotelreservation.repository.GuestRepository;
import com.hotelreservation.repository.LoyaltyAccountRepository;
import com.hotelreservation.repository.LoyaltyTransactionRepository;
import com.hotelreservation.repository.NotificationRepository;
import com.hotelreservation.repository.PaymentRepository;
import com.hotelreservation.repository.ReportRepository;
import com.hotelreservation.repository.ReservationRepository;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.repository.WaitlistRepository;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.security.PasswordService;
import com.hotelreservation.service.BillingPaymentService;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.DashboardService;
import com.hotelreservation.service.DiscountService;
import com.hotelreservation.service.FeedbackService;
import com.hotelreservation.service.GuestManagementService;
import com.hotelreservation.service.LoyaltyService;
import com.hotelreservation.service.NotificationService;
import com.hotelreservation.service.OccupancyService;
import com.hotelreservation.service.PricingService;
import com.hotelreservation.service.ReportService;
import com.hotelreservation.service.ReservationManagementService;
import com.hotelreservation.service.RoomAvailabilityService;
import com.hotelreservation.service.RoomManagementService;
import com.hotelreservation.service.WaitlistService;
import com.hotelreservation.util.ActivityLogService;
import com.hotelreservation.util.JpaUtil;

public final class AppConfig {

    private static final AdminUserRepository
            ADMIN_USER_REPOSITORY = new AdminUserRepository();
    private static final ReservationRepository
            RESERVATION_REPOSITORY = new ReservationRepository();
    private static final RoomRepository ROOM_REPOSITORY =
            new RoomRepository();
    private static final GuestRepository GUEST_REPOSITORY =
            new GuestRepository();
    private static final BillingRepository BILLING_REPOSITORY =
            new BillingRepository();
    private static final AddOnRepository ADD_ON_REPOSITORY =
            new AddOnRepository();
    private static final PaymentRepository PAYMENT_REPOSITORY =
            new PaymentRepository();
    private static final DiscountRepository DISCOUNT_REPOSITORY =
            new DiscountRepository();
    private static final LoyaltyAccountRepository
            LOYALTY_ACCOUNT_REPOSITORY =
            new LoyaltyAccountRepository();
    private static final LoyaltyTransactionRepository
            LOYALTY_TRANSACTION_REPOSITORY =
            new LoyaltyTransactionRepository();
    private static final WaitlistRepository WAITLIST_REPOSITORY =
            new WaitlistRepository();
    private static final FeedbackRepository FEEDBACK_REPOSITORY =
            new FeedbackRepository();
    private static final NotificationRepository
            NOTIFICATION_REPOSITORY =
            new NotificationRepository();
    private static final ReportRepository REPORT_REPOSITORY =
            new ReportRepository();

    private static final PricingService PRICING_SERVICE =
            new PricingService();
    private static final OccupancyService OCCUPANCY_SERVICE =
            new OccupancyService();
    private static final RoomAvailabilityService
            ROOM_AVAILABILITY_SERVICE =
            new RoomAvailabilityService(ROOM_REPOSITORY);

    private static final PasswordService PASSWORD_SERVICE =
            new PasswordService();
    private static final AdminSession ADMIN_SESSION =
            new AdminSession();
    private static final AuthenticationService
            AUTHENTICATION_SERVICE =
            new AuthenticationService(
                    ADMIN_USER_REPOSITORY,
                    PASSWORD_SERVICE,
                    ADMIN_SESSION
            );

    private static final AvailabilityEventPublisher
            AVAILABILITY_PUBLISHER =
            new AvailabilityEventPublisher();
    private static final AdminNotificationObserver
            ADMIN_NOTIFICATION_OBSERVER =
            new AdminNotificationObserver(
                    WAITLIST_REPOSITORY,
                    NOTIFICATION_REPOSITORY,
                    ROOM_REPOSITORY
            );

    private static final LoyaltyService LOYALTY_SERVICE =
            new LoyaltyService(
                    LOYALTY_ACCOUNT_REPOSITORY,
                    LOYALTY_TRANSACTION_REPOSITORY
            );

    private static final ReservationManagementService
            RESERVATION_MANAGEMENT_SERVICE =
            new ReservationManagementService(
                    RESERVATION_REPOSITORY,
                    ROOM_REPOSITORY,
                    GUEST_REPOSITORY,
                    BILLING_REPOSITORY,
                    PRICING_SERVICE,
                    ADMIN_SESSION
            );

    private static final NotificationService
            NOTIFICATION_SERVICE =
            new NotificationService(
                    NOTIFICATION_REPOSITORY,
                    ADMIN_SESSION
            );

    private static final BillingPaymentService
            BILLING_PAYMENT_SERVICE =
            new BillingPaymentService(
                    BILLING_REPOSITORY,
                    PAYMENT_REPOSITORY,
                    RESERVATION_REPOSITORY,
                    ROOM_REPOSITORY,
                    LOYALTY_SERVICE,
                    NOTIFICATION_REPOSITORY,
                    AVAILABILITY_PUBLISHER,
                    ADMIN_SESSION
            );

    private static final DiscountService DISCOUNT_SERVICE =
            new DiscountService(
                    BILLING_REPOSITORY,
                    DISCOUNT_REPOSITORY,
                    PAYMENT_REPOSITORY,
                    ADMIN_SESSION
            );

    private static final RoomManagementService
            ROOM_MANAGEMENT_SERVICE =
            new RoomManagementService(
                    ROOM_REPOSITORY,
                    AVAILABILITY_PUBLISHER,
                    ADMIN_SESSION
            );

    private static final GuestManagementService
            GUEST_MANAGEMENT_SERVICE =
            new GuestManagementService(
                    GUEST_REPOSITORY,
                    LOYALTY_SERVICE,
                    ADMIN_SESSION
            );

    private static final WaitlistService WAITLIST_SERVICE =
            new WaitlistService(
                    WAITLIST_REPOSITORY,
                    GUEST_REPOSITORY,
                    ROOM_REPOSITORY,
                    RESERVATION_MANAGEMENT_SERVICE,
                    NOTIFICATION_REPOSITORY,
                    ADMIN_SESSION
            );

    private static final FeedbackService FEEDBACK_SERVICE =
            new FeedbackService(
                    RESERVATION_REPOSITORY,
                    BILLING_REPOSITORY,
                    PAYMENT_REPOSITORY,
                    FEEDBACK_REPOSITORY,
                    ADMIN_SESSION
            );

    private static final ActivityLogService
            ACTIVITY_LOG_SERVICE =
            new ActivityLogService();

    private static final ReportService REPORT_SERVICE =
            new ReportService(
                    REPORT_REPOSITORY,
                    RESERVATION_REPOSITORY,
                    ROOM_REPOSITORY,
                    ACTIVITY_LOG_SERVICE,
                    ADMIN_SESSION
            );

    private static final DashboardService DASHBOARD_SERVICE =
            new DashboardService(
                    RESERVATION_REPOSITORY,
                    ROOM_REPOSITORY,
                    GUEST_REPOSITORY,
                    BILLING_REPOSITORY,
                    PAYMENT_REPOSITORY,
                    WAITLIST_REPOSITORY,
                    NOTIFICATION_SERVICE,
                    ADMIN_SESSION
            );

    private static final BookingService BOOKING_SERVICE =
            new BookingService(
                    ROOM_AVAILABILITY_SERVICE,
                    OCCUPANCY_SERVICE,
                    PRICING_SERVICE,
                    GUEST_REPOSITORY,
                    RESERVATION_REPOSITORY,
                    BILLING_REPOSITORY,
                    ADD_ON_REPOSITORY,
                    LOYALTY_SERVICE
            );

    private static final AdminAccountSeeder
            ADMIN_ACCOUNT_SEEDER =
            new AdminAccountSeeder(
                    ADMIN_USER_REPOSITORY,
                    PASSWORD_SERVICE
            );
    private static final ApplicationDataSeeder
            APPLICATION_DATA_SEEDER =
            new ApplicationDataSeeder();

    private static boolean initialized;

    private AppConfig() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        APPLICATION_DATA_SEEDER.seedDefaults();
        ADMIN_ACCOUNT_SEEDER.seedDefaultAccounts();
        AVAILABILITY_PUBLISHER.subscribe(
                ADMIN_NOTIFICATION_OBSERVER
        );
        initialized = true;
    }

    public static Object createController(
            Class<?> controllerClass
    ) {
        if (controllerClass == AdminLoginController.class) {
            return new AdminLoginController(
                    AUTHENTICATION_SERVICE
            );
        }
        if (controllerClass
                == AdminNavigationController.class) {
            return new AdminNavigationController(
                    AUTHENTICATION_SERVICE,
                    ADMIN_SESSION
            );
        }
        if (controllerClass
                == AdminDashboardController.class) {
            return new AdminDashboardController(
                    AUTHENTICATION_SERVICE,
                    ADMIN_SESSION,
                    DASHBOARD_SERVICE
            );
        }
        if (controllerClass
                == ReservationManagementController.class) {
            return new ReservationManagementController(
                    AUTHENTICATION_SERVICE,
                    ADMIN_SESSION,
                    RESERVATION_MANAGEMENT_SERVICE,
                    BILLING_PAYMENT_SERVICE
            );
        }
        if (controllerClass
                == ReservationFormController.class) {
            return new ReservationFormController(
                    RESERVATION_MANAGEMENT_SERVICE,
                    BILLING_PAYMENT_SERVICE
            );
        }
        if (controllerClass
                == RoomManagementController.class) {
            return new RoomManagementController(
                    AUTHENTICATION_SERVICE,
                    ADMIN_SESSION,
                    ROOM_MANAGEMENT_SERVICE
            );
        }
        if (controllerClass
                == GuestManagementController.class) {
            return new GuestManagementController(
                    AUTHENTICATION_SERVICE,
                    ADMIN_SESSION,
                    GUEST_MANAGEMENT_SERVICE
            );
        }
        if (controllerClass
                == BillingPaymentController.class) {
            return new BillingPaymentController(
                    AUTHENTICATION_SERVICE,
                    ADMIN_SESSION,
                    BILLING_PAYMENT_SERVICE
            );
        }
        if (controllerClass
                == DiscountManagementController.class) {
            return new DiscountManagementController(
                    AUTHENTICATION_SERVICE,
                    ADMIN_SESSION,
                    DISCOUNT_SERVICE
            );
        }
        if (controllerClass
                == LoyaltyManagementController.class) {
            return new LoyaltyManagementController(
                    AUTHENTICATION_SERVICE,
                    ADMIN_SESSION,
                    LOYALTY_SERVICE
            );
        }
        if (controllerClass == WaitlistController.class) {
            return new WaitlistController(
                    AUTHENTICATION_SERVICE,
                    ADMIN_SESSION,
                    WAITLIST_SERVICE
            );
        }
        if (controllerClass
                == FeedbackManagementController.class) {
            return new FeedbackManagementController(
                    AUTHENTICATION_SERVICE,
                    ADMIN_SESSION,
                    FEEDBACK_SERVICE
            );
        }
        if (controllerClass == ReportsController.class) {
            return new ReportsController(
                    AUTHENTICATION_SERVICE,
                    ADMIN_SESSION,
                    REPORT_SERVICE
            );
        }
        if (controllerClass
                == NotificationsController.class) {
            return new NotificationsController(
                    AUTHENTICATION_SERVICE,
                    ADMIN_SESSION,
                    NOTIFICATION_SERVICE
            );
        }
        if (controllerClass
                == LoyaltyCheckController.class) {
            return new LoyaltyCheckController(
                    LOYALTY_SERVICE
            );
        }
        if (controllerClass
                == FeedbackLookupController.class) {
            return new FeedbackLookupController(
                    FEEDBACK_SERVICE
            );
        }
        if (controllerClass == FeedbackController.class) {
            return new FeedbackController(
                    FEEDBACK_SERVICE
            );
        }
        if (controllerClass == OccupancyController.class) {
            return new OccupancyController(
                    OCCUPANCY_SERVICE
            );
        }
        if (controllerClass == RoomSelectionController.class) {
            return new RoomSelectionController(
                    OCCUPANCY_SERVICE,
                    ROOM_AVAILABILITY_SERVICE
            );
        }
        if (controllerClass == AddOnsController.class) {
            return new AddOnsController(PRICING_SERVICE);
        }
        if (controllerClass
                == BookingSummaryController.class) {
            return new BookingSummaryController(
                    OCCUPANCY_SERVICE,
                    PRICING_SERVICE,
                    BOOKING_SERVICE
            );
        }

        try {
            return controllerClass
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "No controller configuration exists for "
                            + controllerClass.getName(),
                    exception
            );
        }
    }

    public static AuthenticationService
    getAuthenticationService() {
        return AUTHENTICATION_SERVICE;
    }

    public static AdminSession getAdminSession() {
        return ADMIN_SESSION;
    }

    public static ReservationManagementService
    getReservationManagementService() {
        return RESERVATION_MANAGEMENT_SERVICE;
    }

    public static synchronized void shutdown() {
        ADMIN_SESSION.clear();
        AVAILABILITY_PUBLISHER.unsubscribe(
                ADMIN_NOTIFICATION_OBSERVER
        );
        JpaUtil.close();
        initialized = false;
    }
}
