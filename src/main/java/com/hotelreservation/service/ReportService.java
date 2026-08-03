package com.hotelreservation.service;

import com.hotelreservation.model.Billing;
import com.hotelreservation.model.ReportPeriod;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.ReservationRoom;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomType;
import com.hotelreservation.repository.ReportRepository;
import com.hotelreservation.repository.ReservationRepository;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.util.ActivityLogService;
import com.hotelreservation.util.AppLogger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportService {

    private static final Logger LOGGER =
            AppLogger.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final ReservationRepository
            reservationRepository;
    private final RoomRepository roomRepository;
    private final ActivityLogService activityLogService;
    private final AdminSession adminSession;

    public ReportService(
            ReportRepository reportRepository,
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            ActivityLogService activityLogService,
            AdminSession adminSession
    ) {
        this.reportRepository = reportRepository;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.activityLogService = activityLogService;
        this.adminSession = adminSession;
    }

    public List<RevenueReportRow> revenue(
            LocalDate from,
            LocalDate to,
            ReportPeriod period,
            RoomType roomType
    ) {
        validateDates(from, to);
        adminSession.requireCurrentUser();
        ReportPeriod grouping = period == null
                ? ReportPeriod.DAILY
                : period;

        Map<LocalDate, List<Billing>> grouped =
                new TreeMap<>();
        for (Billing billing :
                reportRepository.findBillings(
                        from,
                        to,
                        roomType
                )) {
            LocalDate key = periodStart(
                    billing.getCreatedAt().toLocalDate(),
                    grouping
            );
            grouped.computeIfAbsent(
                    key,
                    ignored -> new ArrayList<>()
            ).add(billing);
        }

        List<RevenueReportRow> rows = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Billing>> entry :
                grouped.entrySet()) {
            Set<Long> reservations = new HashSet<>();
            double subtotal = 0;
            double tax = 0;
            double discount = 0;
            double total = 0;

            for (Billing billing : entry.getValue()) {
                reservations.add(
                        billing.getReservation()
                                .getReservationId()
                );
                subtotal += billing.getSubtotal();
                tax += billing.getTaxAmount();
                discount += billing.getDiscountAmount();
                total += billing.getAmountAfterDiscount();
            }

            rows.add(
                    new RevenueReportRow(
                            periodLabel(entry.getKey(), grouping),
                            reservations.size(),
                            roundMoney(subtotal),
                            roundMoney(tax),
                            roundMoney(discount),
                            roundMoney(total)
                    )
            );
        }

        auditReport("REVENUE_REPORT", rows.size());
        return rows;
    }

    public List<OccupancyReportRow> occupancy(
            LocalDate from,
            LocalDate to,
            ReportPeriod period,
            RoomType roomType
    ) {
        validateDates(from, to);
        adminSession.requireCurrentUser();
        ReportPeriod grouping = period == null
                ? ReportPeriod.DAILY
                : period;

        List<Room> rooms = roomRepository.findAll()
                .stream()
                .filter(room ->
                        roomType == null
                                || room.getRoomType() == roomType
                )
                .toList();
        int totalRooms = rooms.size();
        Set<Long> includedRoomIds = new HashSet<>();
        for (Room room : rooms) {
            includedRoomIds.add(room.getRoomId());
        }

        List<Reservation> reservations =
                reservationRepository
                        .findOverlappingForReport(
                                from,
                                to.plusDays(1)
                        );

        Map<LocalDate, List<DailyOccupancy>> grouped =
                new TreeMap<>();
        LocalDate date = from;
        while (!date.isAfter(to)) {
            Set<Long> occupied = new HashSet<>();
            for (Reservation reservation : reservations) {
                if (reservation.getCheckInDate()
                        .isAfter(date)
                        || !reservation.getCheckOutDate()
                        .isAfter(date)) {
                    continue;
                }

                boolean hadAssignments = false;
                for (ReservationRoom assignment :
                        reservation.getReservationRooms()) {
                    hadAssignments = true;
                    Long roomId =
                            assignment.getRoom().getRoomId();
                    if (includedRoomIds.contains(roomId)) {
                        occupied.add(roomId);
                    }
                }
                if (!hadAssignments
                        && reservation.getRoom() != null
                        && includedRoomIds.contains(
                        reservation.getRoom().getRoomId())) {
                    occupied.add(
                            reservation.getRoom().getRoomId()
                    );
                }
            }

            int occupiedCount = occupied.size();
            int available = Math.max(
                    0,
                    totalRooms - occupiedCount
            );
            double percentage = totalRooms == 0
                    ? 0
                    : occupiedCount * 100.0 / totalRooms;

            grouped.computeIfAbsent(
                    periodStart(date, grouping),
                    ignored -> new ArrayList<>()
            ).add(
                    new DailyOccupancy(
                            available,
                            occupiedCount,
                            percentage
                    )
            );
            date = date.plusDays(1);
        }

        List<OccupancyReportRow> rows =
                new ArrayList<>();
        for (Map.Entry<LocalDate, List<DailyOccupancy>> entry :
                grouped.entrySet()) {
            double availableAverage = entry.getValue()
                    .stream()
                    .mapToInt(DailyOccupancy::roomsAvailable)
                    .average()
                    .orElse(0);
            double occupiedAverage = entry.getValue()
                    .stream()
                    .mapToInt(DailyOccupancy::roomsOccupied)
                    .average()
                    .orElse(0);
            double percentageAverage = entry.getValue()
                    .stream()
                    .mapToDouble(
                            DailyOccupancy::occupancyPercentage
                    )
                    .average()
                    .orElse(0);

            rows.add(
                    new OccupancyReportRow(
                            periodLabel(
                                    entry.getKey(),
                                    grouping
                            ),
                            (int) Math.round(availableAverage),
                            (int) Math.round(occupiedAverage),
                            roundMoney(percentageAverage)
                    )
            );
        }

        auditReport("OCCUPANCY_REPORT", rows.size());
        return rows;
    }

    public List<ActivityLogService.ActivityLogRow>
    activity() {
        adminSession.requireCurrentUser();
        List<ActivityLogService.ActivityLogRow> rows =
                activityLogService.readActivityLogs();
        auditReport("ACTIVITY_LOG_REPORT", rows.size());
        return rows;
    }

    private LocalDate periodStart(
            LocalDate date,
            ReportPeriod period
    ) {
        return switch (period) {
            case DAILY -> date;
            case WEEKLY -> date.with(
                    TemporalAdjusters.previousOrSame(
                            DayOfWeek.MONDAY
                    )
            );
            case MONTHLY -> date.withDayOfMonth(1);
        };
    }

    private String periodLabel(
            LocalDate start,
            ReportPeriod period
    ) {
        return switch (period) {
            case DAILY -> start.toString();
            case WEEKLY -> start + " to "
                    + start.plusDays(6);
            case MONTHLY -> YearMonth.from(start).toString();
        };
    }

    private void validateDates(
            LocalDate from,
            LocalDate to
    ) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Select both report dates."
            );
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "Report end date cannot be before start date."
            );
        }
        if (to.toEpochDay() - from.toEpochDay() > 366) {
            throw new IllegalArgumentException(
                    "Report date range cannot exceed 366 days."
            );
        }
    }

    private void auditReport(String action, int rows) {
        AppLogger.audit(
                LOGGER,
                Level.INFO,
                adminSession.getActorName(),
                action,
                "Report",
                "-",
                "Generated " + rows + " report row(s)."
        );
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record RevenueReportRow(
            String period,
            long reservationCount,
            double subtotal,
            double tax,
            double discounts,
            double total
    ) {
    }

    public record OccupancyReportRow(
            String period,
            int roomsAvailable,
            int roomsOccupied,
            double occupancyPercentage
    ) {
    }

    private record DailyOccupancy(
            int roomsAvailable,
            int roomsOccupied,
            double occupancyPercentage
    ) {
    }
}
