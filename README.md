# Hotel Reservation System

A JavaFX desktop application for a hotel, with two sides: a **guest kiosk** for self-service booking and feedback, and an **admin back office** for reservations, billing, loyalty, waitlists, and reporting. Data is stored with JPA/Hibernate on a file-based H2 database.

Built for APD545 (Seneca Polytechnic). [Add: solo project or team, and your role]

<!-- Add 3-4 screenshots here: kiosk welcome, room selection, admin dashboard, reports -->

## Features

### Guest kiosk

Booking flow: Welcome → Occupancy → Stay Dates → Guest Details → Room Selection → Add-Ons → Loyalty Check → Booking Summary → Confirmation.

- Multi-room booking with availability checked against existing reservations
- Add-ons: Wi-Fi, breakfast, parking, spa, laundry, airport pickup
- Weekday and weekend pricing
- Loyalty lookup by phone number, with optional enrolment at booking
- Guest feedback submission and lookup

Payment is not collected at the kiosk. Bookings are created as pending and paid at the front desk through the admin side.

### Admin back office

- Role-based login (Admin and Manager) with BCrypt-hashed passwords
- Dashboard summary
- Reservation management: create by phone, modify, cancel, search
- Check-in and check-out, payments, refunds, loyalty point redemption, and final bill PDF export
- Discount management, with role limits (Admin up to 15%, Manager up to 30%)
- Refunds above CAD 500 are blocked for the Admin role
- Loyalty accounts: search, transaction history, enrolment
- Waitlist: add, convert to reservation, cancel, with date-overlap matching against the room's next booking
- Notifications: list, mark read, archive
- Reports (occupancy, revenue) with CSV, PDF, and TXT export
- Feedback management: search, summarise, export
- Room management: search and status changes
- Guest management: search and loyalty enrolment

## Design patterns

| Pattern | Where it's used |
|---|---|
| **Strategy** | Pricing (`StandardPricingStrategy`, `WeekendPricingStrategy`) and billing (`StandardBillingStrategy`, `DiscountBillingStrategy`, `LoyaltyBillingStrategy`) |
| **Observer** | Room availability events (`AvailabilityEventPublisher`) notify `AdminNotificationObserver`, which creates admin notifications on checkout and room status changes |
| **Decorator** | Add-on pricing: `BaseBookingPrice` wrapped by `WifiDecorator`, `BreakfastDecorator`, `ParkingDecorator`, `SpaDecorator`, `LaundryDecorator`, `AirportPickupDecorator` |
| **Factory** | `RoomFactory` creates room entities during data seeding |
| **Repository** | `*Repository` classes extend `AbstractRepository` and isolate JPA access |

## Architecture

MVC with a service and repository layer:

```
FXML views  →  Controllers  →  Services  →  Repositories  →  JPA / H2
(kiosk, admin)  (kiosk, admin)   (business rules)  (data access)
```

Controllers are wired manually through a composition root (`AppConfig`) and a controller factory in `SceneNavigator`, with no DI framework. Shared booking validation lives in `ReservationValidator`, which is used by both the kiosk booking flow and admin reservation management.

## Tech stack

| | |
|---|---|
| Language | Java 21 |
| UI | JavaFX 21.0.2 (FXML) |
| Build | Maven, `javafx-maven-plugin` |
| Persistence | Jakarta Persistence 3.1, Hibernate ORM 6.4.4, H2 2.2.224 (file mode) |
| Security | jBCrypt (work factor 12) |
| Logging | SLF4J Simple |

## Data model

`Guest`, `Room`, `Reservation`, `ReservationRoom`, `ReservationAddOn`, `AddOn`, `Billing`, `Payment`, `Discount`, `LoyaltyAccount`, `LoyaltyTransaction`, `Feedback`, `WaitlistEntry`, `AdminUser`, `AdminNotification`

## Getting started

**Prerequisites:** JDK 21 and Maven.

```bash
git clone https://github.com/lamritha/Hotel-Reservation-System.git
cd Hotel-Reservation-System
mvn clean compile
mvn javafx:run
```

Run the commands from the directory that contains `pom.xml`. The database, exports, and logs use relative paths (`./database`, `./exports`, `./logs`), so launching from a different working directory creates a separate, empty database there.

On first launch the app seeds rooms (up to three each of Single, Double, Deluxe, and Penthouse), the add-on catalogue, and two admin accounts.

### Demo admin accounts

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `ChangeMe!2026Admin` |
| Manager | `manager` | `ChangeMe!2026Manager` |

These are seeded demo credentials for local use only.

## Project structure

```
src/main/java/com/hotelreservation/
├── app/          data seeders
├── config/       AppConfig (composition root), HotelPolicyConfig
├── controller/   kiosk/ and admin/ JavaFX controllers
├── decorator/    add-on pricing decorators
├── events/       Observer: availability events and notifications
├── model/        JPA entities and enums
├── repository/   data access
├── security/     authentication, session, password hashing
├── service/      business logic
├── strategy/     pricing and billing strategies
└── util/         validation, export, logging, navigation
src/main/resources/
├── META-INF/persistence.xml
├── views/        FXML (kiosk and admin)
└── css/
```

## Testing

There is no automated test suite. `src/test` contains standalone `main`-method verification scripts (for example `VerifyFullBackend`, `VerifyExports`, `VerifyWaitlistDateMatch`) that exercise the services against a real database and are run manually. They are not JUnit tests and don't run under `mvn test`.

## Known limitations

- No payment collection at the kiosk; payment is handled by staff in the admin billing screen
- Room management supports search and status changes only (no create, edit, or delete)
- Guest management supports search and loyalty enrolment only
- Admin notifications are broadcast to all admins rather than to individual users
- Pricing rules and hotel policies are constants in `HotelPolicyConfig` and `RoomType`, not configurable through the UI
