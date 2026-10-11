package com.portal.movies.booking;

import com.portal.movies.domain.*;
import com.portal.movies.repository.*;
import com.portal.movies.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BookingService {
    private static final int HOLD_DURATION_SECONDS = 300;
    private final ShowSeatRepository seats;
    private final MovieShowtimeRepository showtimes;
    private final AppUserRepository users;
    private final BookingRepository bookings;

    public BookingService(ShowSeatRepository seats, MovieShowtimeRepository showtimes, AppUserRepository users, BookingRepository bookings) {
        this.seats=seats; this.showtimes=showtimes; this.users=users; this.bookings=bookings;
    }

    /**
     * All requested seat rows are locked in id order inside one transaction. The lock is in MySQL,
     * so it coordinates requests even when the API runs on multiple application instances.
     */
    @Transactional
    public BookingDtos.HoldResponse createHold(Long showtimeId, Long userId, BookingDtos.HoldRequest request) {
        if (!showtimes.existsById(showtimeId)) throw ApiException.notFound("Showtime not found.");
        users.findById(userId).orElseThrow(() -> ApiException.unauthorized("User account no longer exists."));

        List<String> labels = request.seats().stream().map(s -> s.trim().toUpperCase(Locale.ROOT)).distinct().sorted().toList();
        if (labels.size() != request.seats().size()) throw ApiException.badRequest("A seat can only be selected once.");
        List<ShowSeat> lockedSeats = seats.lockSeats(showtimeId, labels);
        if (lockedSeats.size() != labels.size()) throw ApiException.badRequest("One or more seat labels are invalid for this show.");

        LocalDateTime now = LocalDateTime.now();
        for (ShowSeat seat : lockedSeats) {
            boolean claimable = seat.getStatus() == SeatStatus.AVAILABLE || seat.holdHasExpired(now);
            if (!claimable) throw ApiException.conflict("Seat " + seat.getSeatLabel() + " is no longer available. Refresh the seat map.");
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = now.plusSeconds(HOLD_DURATION_SECONDS);
        lockedSeats.forEach(seat -> seat.hold(token, userId, expiresAt, labels.size()));
        seats.saveAll(lockedSeats);
        return new BookingDtos.HoldResponse(token, labels, expiresAt, HOLD_DURATION_SECONDS);
    }

    @Transactional
    public BookingDtos.BookingView confirmHold(Long userId, BookingDtos.ConfirmRequest request) {
        List<ShowSeat> heldSeats = seats.lockSeatsByHold(request.holdToken().trim(), userId);
        if (heldSeats.isEmpty()) throw ApiException.conflict("This seat hold has expired or does not belong to your account.");
        LocalDateTime now = LocalDateTime.now();
        if (heldSeats.stream().anyMatch(s -> s.getStatus() != SeatStatus.HELD || s.holdHasExpired(now)))
            throw ApiException.conflict("Your seat hold has expired. Please select seats again.");
        if (heldSeats.getFirst().getHoldSize() != heldSeats.size())
            throw ApiException.conflict("This hold is no longer complete. Please select seats again.");

        Long showtimeId = heldSeats.getFirst().getShowtime().getId();
        if (heldSeats.stream().anyMatch(s -> !s.getShowtime().getId().equals(showtimeId)))
            throw ApiException.conflict("The selected seats do not belong to one show.");
        AppUser user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("User account no longer exists."));
        MovieShowtime showtime = showtimes.findById(showtimeId).orElseThrow(() -> ApiException.notFound("Showtime not found."));
        BigDecimal total = BigDecimal.ZERO;
        for (ShowSeat seat : heldSeats) {
            total = total.add(seat.getPriceAmount());
        }
        String code = "MT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        Booking booking = bookings.saveAndFlush(new Booking(code, user, showtime, total));
        for (ShowSeat seat : heldSeats) {
            booking.addSeat(new BookingSeat(seat.getSeatLabel(), seat.getPriceAmount()));
            seat.book(booking);
        }
        bookings.save(booking);
        seats.saveAll(heldSeats);
        return view(booking);
    }

    @Transactional
    public void releaseHold(Long userId, String holdToken) {
        List<ShowSeat> heldSeats = seats.lockSeatsByHold(holdToken, userId);
        if (heldSeats.isEmpty()) return; // Safe to retry a release.
        for (ShowSeat seat : heldSeats) {
            seat.release();
        }
        seats.saveAll(heldSeats);
    }

    @Transactional(readOnly = true)
    public List<BookingDtos.BookingView> myBookings(Long userId) {
        return bookings.findByUser_IdOrderByBookedAtDesc(userId).stream().map(this::view).toList();
    }

    private BookingDtos.BookingView view(Booking b) {
        List<String> labels = new ArrayList<>();
        for (BookingSeat seat : b.getSeats()) {
            labels.add(seat.getSeatLabel());
        }
        Collections.sort(labels);
        return new BookingDtos.BookingView(b.getId(), b.getBookingCode(), b.getShowtime().getMovie().getTitle(),
                b.getShowtime().getStartsAt(), b.getShowtime().getVenue(), b.getShowtime().getScreenName(), labels,
                b.getTotalAmount(), b.getBookedAt());
    }
}
