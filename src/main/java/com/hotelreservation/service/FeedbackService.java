package com.hotelreservation.service;

import com.hotelreservation.config.HotelPolicyConfig;
import com.hotelreservation.model.Billing;
import com.hotelreservation.model.Feedback;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.ReservationStatus;
import com.hotelreservation.model.SentimentTag;
import com.hotelreservation.repository.BillingRepository;
import com.hotelreservation.repository.FeedbackRepository;
import com.hotelreservation.repository.PaymentRepository;
import com.hotelreservation.repository.ReservationRepository;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.util.AppLogger;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FeedbackService {

    private static final Logger LOGGER =
            AppLogger.getLogger(FeedbackService.class);

    private final ReservationRepository reservationRepository;
    private final BillingRepository billingRepository;
    private final PaymentRepository paymentRepository;
    private final FeedbackRepository feedbackRepository;
    private final AdminSession adminSession;

    public FeedbackService(
            ReservationRepository reservationRepository,
            BillingRepository billingRepository,
            PaymentRepository paymentRepository,
            FeedbackRepository feedbackRepository,
            AdminSession adminSession
    ) {
        this.reservationRepository = reservationRepository;
        this.billingRepository = billingRepository;
        this.paymentRepository = paymentRepository;
        this.feedbackRepository = feedbackRepository;
        this.adminSession = adminSession;
    }

    public FeedbackEligibility checkEligibility(
            String lookup
    ) {
        if (lookup == null || lookup.trim().isBlank()) {
            return FeedbackEligibility.ineligible(
                    "Enter a reservation ID or phone number."
            );
        }

        Reservation reservation =
                reservationRepository
                        .findForFeedbackLookup(lookup.trim())
                        .orElse(null);
        if (reservation == null) {
            return FeedbackEligibility.ineligible(
                    "No matching reservation was found."
            );
        }
        if (reservation.getStatus()
                != ReservationStatus.CHECKED_OUT) {
            return FeedbackEligibility.ineligible(
                    "Feedback is available only after checkout."
            );
        }
        if (feedbackRepository
                .findByReservationId(
                        reservation.getReservationId()
                )
                .isPresent()) {
            return FeedbackEligibility.ineligible(
                    "Feedback was already submitted for this reservation."
            );
        }

        Billing billing = billingRepository
                .findByReservationId(
                        reservation.getReservationId()
                )
                .orElse(null);
        if (billing == null) {
            return FeedbackEligibility.ineligible(
                    "The final bill could not be verified."
            );
        }

        double paid = paymentRepository.getNetPaid(
                billing.getBillingId()
        );
        if (billing.getAmountAfterDiscount() - paid > 0.009) {
            return FeedbackEligibility.ineligible(
                    "Feedback is available after the balance is fully settled."
            );
        }

        return FeedbackEligibility.eligible(reservation);
    }

    public Feedback submit(
            Long reservationId,
            int rating,
            String comment
    ) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                    "Rating must be between 1 and 5."
            );
        }
        String normalized =
                comment == null ? "" : comment.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Enter a feedback comment."
            );
        }
        if (normalized.length()
                > HotelPolicyConfig
                .FEEDBACK_COMMENT_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Feedback comment cannot exceed "
                            + HotelPolicyConfig
                            .FEEDBACK_COMMENT_MAX_LENGTH
                            + " characters."
            );
        }

        FeedbackEligibility eligibility =
                checkEligibility("RES-" + reservationId);
        if (!eligibility.eligible()) {
            throw new IllegalStateException(
                    eligibility.message()
            );
        }

        Reservation reservation =
                eligibility.reservation();
        SentimentTag sentiment =
                determineSentiment(rating, normalized);
        Feedback feedback = feedbackRepository.save(
                new Feedback(
                        reservation,
                        reservation.getGuest(),
                        rating,
                        normalized,
                        sentiment
                )
        );

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                "GUEST",
                "FEEDBACK_SUBMITTED",
                "Reservation",
                String.valueOf(reservationId),
                "Rating " + rating
                        + " submitted with sentiment "
                        + sentiment + "."
        );
        return feedback;
    }

    public List<Feedback> searchAdminFeedback(
            String guest,
            Integer rating,
            SentimentTag sentiment,
            LocalDate from,
            LocalDate to
    ) {
        adminSession.requireCurrentUser();
        List<Feedback> results =
                feedbackRepository.search(
                        guest,
                        rating,
                        sentiment,
                        from,
                        to
                );

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                adminSession.getActorName(),
                "FEEDBACK_SEARCH",
                "Feedback",
                "-",
                "Feedback search returned "
                        + results.size() + " row(s)."
        );
        return results;
    }

    public double averageRating(List<Feedback> feedback) {
        return feedback.stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0);
    }

    public FeedbackSummary summarize(
            List<Feedback> feedback
    ) {
        List<Feedback> values = feedback == null
                ? List.of()
                : feedback;
        Map<SentimentTag, Long> counts =
                new EnumMap<>(SentimentTag.class);
        for (SentimentTag tag : SentimentTag.values()) {
            counts.put(tag, 0L);
        }
        for (Feedback entry : values) {
            counts.computeIfPresent(
                    entry.getSentimentTag(),
                    (tag, count) -> count + 1
            );
        }
        return new FeedbackSummary(
                values.size(),
                averageRating(values),
                Map.copyOf(counts)
        );
    }

    private SentimentTag determineSentiment(
            int rating,
            String comment
    ) {
        String value = comment.toLowerCase(Locale.ROOT);
        if (value.contains("clean")
                || value.contains("dirty")) {
            return SentimentTag.CLEANLINESS;
        }
        if (value.contains("staff")
                || value.contains("service")) {
            return SentimentTag.SERVICE;
        }
        if (value.contains("noise")
                || value.contains("loud")) {
            return SentimentTag.NOISE;
        }
        if (value.contains("bill")
                || value.contains("charge")) {
            return SentimentTag.BILLING;
        }
        if (rating >= 4) {
            return SentimentTag.POSITIVE;
        }
        if (rating == 3) {
            return SentimentTag.NEUTRAL;
        }
        return SentimentTag.NEEDS_REVIEW;
    }

    public record FeedbackEligibility(
            boolean eligible,
            String message,
            Reservation reservation
    ) {
        public static FeedbackEligibility eligible(
                Reservation reservation
        ) {
            return new FeedbackEligibility(
                    true,
                    "Reservation is eligible for feedback.",
                    reservation
            );
        }

        public static FeedbackEligibility ineligible(
                String message
        ) {
            return new FeedbackEligibility(
                    false,
                    message,
                    null
            );
        }
    }

    public record FeedbackSummary(
            int feedbackCount,
            double averageRating,
            Map<SentimentTag, Long> sentimentCounts
    ) {
        public long count(SentimentTag tag) {
            return sentimentCounts.getOrDefault(tag, 0L);
        }
    }
}
