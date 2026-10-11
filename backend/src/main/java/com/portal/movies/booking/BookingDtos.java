package com.portal.movies.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public final class BookingDtos {
    private BookingDtos() {}
    public record HoldRequest(@NotEmpty(message="Select at least one seat.") @Size(max=8, message="You can select up to 8 seats at once.") List<@NotBlank String> seats) {}
    public record HoldResponse(String holdToken, List<String> seats, LocalDateTime expiresAt, int holdDurationSeconds) {}
    public record ConfirmRequest(@NotBlank(message="Hold token is required.") String holdToken) {}
    public record BookingView(Long id, String bookingCode, String movieTitle, LocalDateTime startsAt,
                              String venue, String screenName, List<String> seats, BigDecimal totalAmount, Instant bookedAt) {}
}
